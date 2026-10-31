package com.mfish.erp.commande;

import jakarta.validation.constraints.NotNull;

/**
 * Changement de statut d'une commande.
 */
public record StatutCommandeRequest(@NotNull StatutCommande statut) {
}
