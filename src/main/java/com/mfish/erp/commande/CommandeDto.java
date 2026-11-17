package com.mfish.erp.commande;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representation publique d'une commande (avec ses lignes).
 */
public record CommandeDto(
        UUID id,
        UUID clientId,
        StatutCommande statut,
        LocalDate dateLivraisonSouhaitee,
        long montantTotal,
        ModePaiement modePaiement,
        StatutPaiement statutPaiement,
        long montantPaye,
        String notes,
        SourceCommande source,
        List<LigneCommandeDto> lignes) {

    public static CommandeDto from(final Commande commande) {
        return new CommandeDto(
                commande.getId(),
                commande.getClient().getId(),
                commande.getStatut(),
                commande.getDateLivraisonSouhaitee(),
                commande.getMontantTotal(),
                commande.getModePaiement(),
                commande.getStatutPaiement(),
                commande.getMontantPaye(),
                commande.getNotes(),
                commande.getSource(),
                commande.getLignes().stream().map(LigneCommandeDto::from).toList());
    }
}
