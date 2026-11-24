package com.mfish.erp.ia;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.CommandeService;
import com.mfish.erp.commande.LigneCommande;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commun.AppelExterneException;
import com.mfish.erp.conversation.EtatCommande;
import com.mfish.erp.conversation.StatutEtatCommande;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import com.mfish.erp.tournee.Coordonnees;
import com.mfish.erp.tournee.GeocodingPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestration du traitement IA : prompt enrichi de l'etat de la commande, appel LLM, parsing
 * (avec re-essai), fusion dans la memoire de commande, validation (produits, quartier, prix en
 * base) puis creation de commande, question sur les seuls champs manquants ou escalade.
 */
@Service
public class OrchestrateurIa {

    private static final Logger LOG = LoggerFactory.getLogger(OrchestrateurIa.class);

    private static final String MESSAGE_LIEU_A_PRECISER = "Preciser le lieu de livraison";

    /** Contexte geographique ajoute au quartier avant l'appel a Google Geocoding. */
    private static final String CONTEXTE_GEOCODAGE = ", Yaoundé, Cameroun";

    private static final String MESSAGE_MODIFICATION =
            "Votre commande est deja enregistree. Je transmets votre demande de modification "
                    + "au commercant qui va vous recontacter.";

    private final LlmPort llmPort;
    private final PromptBuilder promptBuilder;
    private final ParseurReponseIa parseur;
    private final MemoireCommande memoire;
    private final RapprochementProduits rapprochement;
    private final RapprochementQuartiers rapprochementQuartiers;
    private final GeocodingPort geocodingPort;
    private final ProduitRepository produitRepository;
    private final QuartierRepository quartierRepository;
    private final ClientRepository clientRepository;
    private final CommandeService commandeService;
    private final Clock clock;

    public OrchestrateurIa(final LlmPort llmPort,
                           final PromptBuilder promptBuilder,
                           final ParseurReponseIa parseur,
                           final MemoireCommande memoire,
                           final RapprochementProduits rapprochement,
                           final RapprochementQuartiers rapprochementQuartiers,
                           final GeocodingPort geocodingPort,
                           final ProduitRepository produitRepository,
                           final QuartierRepository quartierRepository,
                           final ClientRepository clientRepository,
                           final CommandeService commandeService,
                           final Clock clock) {
        this.llmPort = llmPort;
        this.promptBuilder = promptBuilder;
        this.parseur = parseur;
        this.memoire = memoire;
        this.rapprochement = rapprochement;
        this.rapprochementQuartiers = rapprochementQuartiers;
        this.geocodingPort = geocodingPort;
        this.produitRepository = produitRepository;
        this.quartierRepository = quartierRepository;
        this.clientRepository = clientRepository;
        this.commandeService = commandeService;
        this.clock = clock;
    }

    /** Traitement sans memoire de commande (le client repart d'une commande vide). */
    public ResultatIa traiter(final Client client, final List<String> historique,
                              final String message) {
        return traiter(client, EtatCommande.vide(), historique, message);
    }

    /**
     * Traitement d'un message en tenant compte de l'etat de commande deja accumule.
     */
    public ResultatIa traiter(final Client client, final EtatCommande etatCourant,
                              final List<String> historique, final String message) {
        final EtatCommande etat = etatCourant == null ? EtatCommande.vide() : etatCourant;
        final List<Produit> produits = produitRepository.findByActifTrueOrderByNomAsc();
        final List<Quartier> quartiers = quartierRepository.findByActifTrueOrderByOrdreDefautAsc();
        final String prompt = promptBuilder.construire(produits, quartiers, joursLivraison(), etat);
        final List<String> tousMessages = new ArrayList<>(historique);
        tousMessages.add(message);
        final List<String> contexte = promptBuilder.limiterContexte(tousMessages);

        final ReponseIa reponse = appelerAvecRetry(prompt, contexte);
        final Intention intention = reponse.intentionEnum();

        if (reponse.escalade()) {
            return new ResultatIa(reponse.reponse(), true, false, etat);
        }
        if (etat.estEnregistree() && intention == Intention.MODIFICATION) {
            // Une commande enregistree ne se modifie pas par chat : un humain reprend la main.
            return new ResultatIa(MESSAGE_MODIFICATION, true, false, etat);
        }

        final EtatCommande fusionne = memoire.fusionner(etat, reponse, message);
        if (intention == Intention.ANNULATION) {
            return new ResultatIa(texteOu(reponse.reponse(), "Votre commande est annulee."),
                    false, false, fusionne);
        }

        // Commande deja enregistree : merci/remerciement bref ou simple recapitulatif.
        if (fusionne.estEnregistree()) {
            if (memoire.estRemerciementOuSalutation(message)) {
                return new ResultatIa(memoire.reponsePolie(), false, false, fusionne);
            }
            return new ResultatIa(texteOu(reponse.reponse(), memoire.recapitulatif(fusionne)),
                    false, false, fusionne);
        }

        // Informations encore incompletes : on repond aux questions directes (prix, etc.) et on
        // ne demande que les informations reellement manquantes. La reponse naturelle du LLM est
        // prioritaire ; la question deterministe ne sert que de filet de securite.
        if (!fusionne.complet()) {
            final String reponseFinale = texteOu(reponse.reponse(), memoire.questionSuivante(fusionne));
            return new ResultatIa(reponseFinale, false, false, fusionne);
        }

        // Commande complete : on ne sauvegarde que lorsque le client a explicitement confirme.
        // Sinon on presente le recapitulatif et on attend sa confirmation.
        if (!memoire.estConfirmation(message)) {
            final EtatCommande aConfirmer = fusionne.avecStatut(StatutEtatCommande.A_CONFIRMER);
            return new ResultatIa(memoire.recapitulatifAConfirmer(aConfirmer), false, false, aConfirmer);
        }

        try {
            creerCommande(client, memoire.versCommandeIa(fusionne), produits, quartiers);
            final EtatCommande enregistree = fusionne.avecStatut(StatutEtatCommande.ENREGISTREE);
            return new ResultatIa(texteOu(reponse.reponse(), memoire.recapitulatif(enregistree)),
                    false, true, enregistree);
        } catch (ProduitInconnuException | ProduitAmbiguException e) {
            LOG.error("Echec de l'enregistrement de la commande (produit introuvable ou ambigu)", e);
            final EtatCommande echec = fusionne.avecStatut(StatutEtatCommande.ECHEC_ENREGISTREMENT);
            return new ResultatIa(memoire.messageEchecEnregistrement(echec), true, false, echec);
        }
    }

    private static String texteOu(final String texte, final String defaut) {
        return texte == null || texte.isBlank() ? defaut : texte;
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
     * livraison. Si le quartier est inconnu, il est geocode au mieux puis enregistre (il ne sera
     * plus jamais redemande). Le geocodage n'est jamais bloquant : un quartier valide indique par
     * le client ne doit pas faire echouer l'enregistrement de la commande.
     */
    private String associerQuartier(final Client client, final CommandeIa commande,
                                    final List<Quartier> quartiers) {
        final Optional<RapprochementQuartiers.QuartierRapproche> rapproche =
                rapprochementQuartiers.rapprocher(commande.quartier(), quartiers);
        if (rapproche.isEmpty()) {
            return associerQuartierInconnu(client, commande);
        }
        final RapprochementQuartiers.QuartierRapproche resultat = rapproche.get();
        associer(client, resultat.quartier(), commande.adresse());
        return resultat.ambigu() ? MESSAGE_LIEU_A_PRECISER : null;
    }

    /**
     * Quartier absent de la base : on tente un geocodage Google pour le persister avec ses
     * coordonnees. Si le geocodage echoue ou est indisponible, le quartier est tout de meme
     * enregistre (sans coordonnees) afin que la commande ne soit jamais bloquee par cette etape
     * non essentielle. Retourne toujours {@code null} (pas d'ambiguite bloquante).
     */
    private String associerQuartierInconnu(final Client client, final CommandeIa commande) {
        if (commande.quartier() == null || commande.quartier().isBlank()) {
            return null;
        }
        final Optional<Coordonnees> coordonnees = geocoder(commande.quartier());
        final Quartier nouveau = coordonnees
                .map(c -> new Quartier(commande.quartier(), null, c.latitude(), c.longitude(), 0, true))
                .orElseGet(() -> new Quartier(commande.quartier(), null, null, null, 0, true));
        associer(client, quartierRepository.save(nouveau), commande.adresse());
        return null;
    }

    /**
     * Geocode un quartier en ajoutant le contexte geographique. Une indisponibilite du
     * fournisseur (quota, facturation non activee, panne reseau) est traitee comme
     * « aucun resultat » : le quartier sera enregistre sans coordonnees plutot que de faire
     * echouer la commande.
     */
    private Optional<Coordonnees> geocoder(final String quartier) {
        try {
            return geocodingPort.geocoder(quartier + CONTEXTE_GEOCODAGE);
        } catch (AppelExterneException e) {
            LOG.warn("Geocodage indisponible pour {} : enregistrement sans coordonnees", quartier, e);
            return Optional.empty();
        }
    }

    private void associer(final Client client, final Quartier quartier, final String adresse) {
        client.setQuartier(quartier);
        client.setAdresse(adresse);
        clientRepository.save(client);
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
