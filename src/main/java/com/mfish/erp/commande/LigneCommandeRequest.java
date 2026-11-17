package com.mfish.erp.commande;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * Ligne d'une commande a creer. Le prix est lu en base (jamais fourni par l'appelant).
 */
public record LigneCommandeRequest(
        @NotNull UUID produitId,
        @Positive int quantite) {
}
