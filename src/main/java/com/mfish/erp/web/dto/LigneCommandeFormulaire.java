package com.mfish.erp.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * Ligne d'une commande saisie dans le formulaire (le prix est lu en base).
 * La quantite est nullable dans le formulaire (produit non commande) ; seules les
 * lignes a quantite strictement positive sont conservees.
 */
public record LigneCommandeFormulaire(
        @NotNull(message = "validation.obligatoire") UUID produitId,
        @Positive(message = "validation.positif") Integer quantite) {
}
