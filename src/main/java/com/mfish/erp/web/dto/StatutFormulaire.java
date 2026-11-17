package com.mfish.erp.web.dto;

import com.mfish.erp.commande.StatutCommande;
import jakarta.validation.constraints.NotNull;

/**
 * Formulaire de changement de statut d'une commande.
 */
public record StatutFormulaire(@NotNull(message = "validation.obligatoire") StatutCommande statut) {
}
