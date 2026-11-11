package com.mfish.erp.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Formulaire de creation/modification d'un produit.
 *
 * <p>Le fichier image televerse est porte separement par un parametre
 * {@code imageFichier} (multipart) ; ce record ne contient que le chemin de
 * l'image deja stockee ({@code imageExistante}).
 */
public record ProduitFormulaire(
        @NotBlank(message = "validation.obligatoire") String nom,
        @PositiveOrZero(message = "validation.positifOuZero") long prix,
        String unite,
        Boolean actif,
        String imageExistante) {
}
