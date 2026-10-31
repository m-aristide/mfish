package com.mfish.erp.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Formulaire de creation/modification d'un produit.
 */
public record ProduitFormulaire(
        @NotBlank(message = "validation.obligatoire") String nom,
        @PositiveOrZero(message = "validation.positifOuZero") long prix,
        String unite,
        boolean actif) {
}
