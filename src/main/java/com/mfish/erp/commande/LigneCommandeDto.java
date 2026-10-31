package com.mfish.erp.commande;

import java.util.UUID;

/**
 * Representation publique d'une ligne de commande.
 */
public record LigneCommandeDto(UUID id, UUID produitId, int quantite, long prixUnitaire) {

    public static LigneCommandeDto from(final LigneCommande ligne) {
        return new LigneCommandeDto(ligne.getId(), ligne.getProduit().getId(),
                ligne.getQuantite(), ligne.getPrixUnitaire());
    }
}
