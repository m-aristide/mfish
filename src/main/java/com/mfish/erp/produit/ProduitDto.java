package com.mfish.erp.produit;

import java.util.UUID;

/**
 * Representation publique d'un produit.
 */
public record ProduitDto(UUID id, String nom, long prix, String unite, boolean actif) {

    public static ProduitDto from(final Produit produit) {
        return new ProduitDto(produit.getId(), produit.getNom(), produit.getPrix(),
                produit.getUnite(), produit.isActif());
    }
}
