package com.mfish.erp.commande;

import com.mfish.erp.client.Client;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Regles metier des commandes : creation, machine a etats, encaissement, annulation.
 * Les prix sont toujours lus/lies aux lignes (figes) ; aucun montant n'est calcule en
 * {@code double}/{@code float}.
 */
@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;

    public CommandeService(final CommandeRepository commandeRepository) {
        this.commandeRepository = commandeRepository;
    }

    /**
     * Cree une commande en statut {@code A_PLANIFIER} et calcule son montant total.
     */
    public Commande creer(Client client, List<LigneCommande> lignes,
                          ModePaiement modePaiement, SourceCommande source) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(modePaiement, "modePaiement");
        Objects.requireNonNull(source, "source");
        if (lignes == null || lignes.isEmpty()) {
            throw new CommandeInvalideException("Une commande doit contenir au moins une ligne");
        }
        for (final LigneCommande ligne : lignes) {
            if (ligne.getQuantite() <= 0) {
                throw new CommandeInvalideException("La quantite doit etre strictement positive");
            }
            if (ligne.getPrixUnitaire() < 0) {
                throw new CommandeInvalideException("Le prix unitaire ne peut pas etre negatif");
            }
        }

        final long total = lignes.stream().mapToLong(LigneCommande::getSousTotal).sum();
        final boolean dejaPaye = modePaiement == ModePaiement.DEJA_PAYE;
        final StatutPaiement statutPaiement = dejaPaye ? StatutPaiement.PAYE : StatutPaiement.NON_PAYE;

        final Commande commande = new Commande(client, StatutCommande.A_PLANIFIER, modePaiement,
                statutPaiement, source);
        commande.setMontantTotal(total);
        if (dejaPaye) {
            commande.setMontantPaye(total);
        }
        lignes.forEach(commande::ajouterLigne);
        return commandeRepository.save(commande);
    }

    /**
     * Applique une transition de statut uniquement si elle est valide.
     */
    public Commande changerStatut(Commande commande, StatutCommande cible) {
        Objects.requireNonNull(commande, "commande");
        Objects.requireNonNull(cible, "cible");
        if (!commande.getStatut().peutPasserA(cible)) {
            throw new TransitionStatutInvalideException(commande.getStatut(), cible);
        }
        commande.setStatut(cible);
        return commandeRepository.save(commande);
    }

    /**
     * Enregistre un paiement partiel ou complet. Le surpaiement est refuse.
     */
    public Commande enregistrerPaiement(Commande commande, long montant) {
        Objects.requireNonNull(commande, "commande");
        if (montant <= 0) {
            throw new CommandeInvalideException("Le montant paye doit etre strictement positif");
        }
        final long nouveauMontant = commande.getMontantPaye() + montant;
        if (nouveauMontant > commande.getMontantTotal()) {
            throw new SurpaiementException(commande.getMontantTotal(), nouveauMontant);
        }
        commande.setMontantPaye(nouveauMontant);
        commande.setStatutPaiement(nouveauMontant == commande.getMontantTotal()
                ? StatutPaiement.PAYE
                : StatutPaiement.PARTIEL);
        return commandeRepository.save(commande);
    }

    /**
     * Annule une commande (refuse si elle est deja livree).
     */
    public Commande annuler(Commande commande) {
        return changerStatut(commande, StatutCommande.ANNULEE);
    }
}
