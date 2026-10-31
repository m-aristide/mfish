package com.mfish.erp.produit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Entree de creation/modification d'un produit.
 */
public record ProduitRequest(
        @NotBlank String nom,
        @PositiveOrZero long prix,
        String unite,
        boolean actif) {
}
