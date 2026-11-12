package com.mfish.erp.ia;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.CommandeService;
import com.mfish.erp.commande.LigneCommande;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Orchestration du traitement IA : prompt, appel LLM, parsing (avec re-essai), validation
 * (produits, quartier, prix en base) puis creation de commande ou escalade.
 */
@Service
public class OrchestrateurIa {

    private static final String MESSAGE_LIEU_A_PRECISER = "Preciser le lieu de livraison";

    private final LlmPort llmPort;
    private final PromptBuilder promptBuilder;
    private final ParseurReponseIa parseur;
    private final RapprochementProduits rapprochement;
    private final RapprochementQuartiers rapprochementQuartiers;
    private final ProduitRepository produitRepository;
    private final QuartierRepository quartierRepository;
    private final ClientRepository clientRepository;
    private final CommandeService commandeService;
    private final Clock clock;

    public OrchestrateurIa(final LlmPort llmPort,
                           final PromptBuilder promptBuilder,
                           final ParseurReponseIa parseur,
                           final RapprochementProduits rapprochement,
                           final RapprochementQuartiers rapprochementQuartiers,
                           final ProduitRepository produitRepository,
                           final QuartierRepository quartierRepository,
                           final ClientRepository clientRepository,
                           final CommandeService commandeService,
                           final Clock clock) {
        this.llmPort = llmPort;
        this.promptBuilder = promptBuilder;
        this.parseur = parseur;
        this.rapprochement = rapprochement;
        this.rapprochementQuartiers = rapprochementQuartiers;
        this.produitRepository = produitRepository;
        this.quartierRepository = quartierRepository;
        this.clientRepository = clientRepository;
        this.commandeService = commandeService;
        this.clock = clock;
    }

    public ResultatIa traiter(final Client client, final List<String> historique, final String message) {
        final List<Produit> produits = produitRepository.findByActifTrueOrderByNomAsc();
        final List<Quartier> quartiers = quartierRepository.findByActifTrueOrderByOrdreDefautAsc();
        final String prompt = promptBuilder.construire(produits, quartiers, joursLivraison());
        final List<String> tousMessages = new ArrayList<>(historique);
        tousMessages.add(message);
        final List<String> contexte = promptBuilder.limiterContexte(tousMessages);

        final ReponseIa reponse = appelerAvecRetry(prompt, contexte);

        if (reponse.escalade()) {
            return new ResultatIa(reponse.reponse(), true, false);
        }
        if (reponse.intentionEnum() != Intention.COMMANDE
                || reponse.commande() == null
                || !reponse.commande().complet()) {
            return new ResultatIa(reponse.reponse(), false, false);
        }
        try {
            creerCommande(client, reponse.commande(), produits, quartiers);
            return new ResultatIa(reponse.reponse(), false, true);
        } catch (ProduitInconnuException | ProduitAmbiguException | QuartierInconnuException e) {
            return new ResultatIa("Je n'ai pas pu enregistrer votre commande, le commercant va vous recontacter.",
                    true, false);
        }
    }

    private List<LocalDate> joursLivraison() {
        final LocalDate aujourdhui = LocalDate.now(clock);
        final List<LocalDate> jours = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            jours.add(aujourdhui.plusDays(i));
        }
        return jours;
    }

    private ReponseIa appelerAvecRetry(final String prompt, final List<String> contexte) {
        final String brut = llmPort.generer(prompt, contexte);
        try {
            return parseur.parser(brut);
        } catch (ReponseIaInvalideException e) {
            final String secondEssai = llmPort.generer(prompt, contexte);
            try {
                return parseur.parser(secondEssai);
            } catch (ReponseIaInvalideException e2) {
                return new ReponseIa("Je vous mets en relation avec le commercant.", "autre", null, true);
            }
        }
    }

    private void creerCommande(final Client client, final CommandeIa commande,
                               final List<Produit> produits, final List<Quartier> quartiers) {
        final String notes = associerQuartier(client, commande, quartiers);
        final List<RapprochementProduits.ProduitRapproche> rapproches =
                rapprochement.rapprocher(commande.produits(), produits);
        final List<LigneCommande> lignes = rapproches.stream()
                .map(r -> new LigneCommande(r.produit(), r.quantite(), r.produit().getPrix()))
                .toList();
        commandeService.creer(client, lignes, ModePaiement.ESPECES, SourceCommande.WHATSAPP_IA,
                parseDate(commande.dateSouhaitee()), notes);
    }

    /**
     * Associe au client le quartier le plus probable de la base. Si plusieurs quartiers
     * correspondent, prend le premier et retourne une note demandant de preciser le lieu de
     * livraison. Leve {@link QuartierInconnuException} si le quartier fourni est introuvable.
     */
    private String associerQuartier(final Client client, final CommandeIa commande,
                                    final List<Quartier> quartiers) {
        final Optional<RapprochementQuartiers.QuartierRapproche> rapproche =
                rapprochementQuartiers.rapprocher(commande.quartier(), quartiers);
        if (rapproche.isEmpty()) {
            if (commande.quartier() != null && !commande.quartier().isBlank()) {
                throw new QuartierInconnuException(commande.quartier());
            }
            return null;
        }
        final RapprochementQuartiers.QuartierRapproche resultat = rapproche.get();
        client.setQuartier(resultat.quartier());
        client.setAdresse(commande.adresse());
        clientRepository.save(client);
        return resultat.ambigu() ? MESSAGE_LIEU_A_PRECISER : null;
    }

    private LocalDate parseDate(final String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valeur);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
