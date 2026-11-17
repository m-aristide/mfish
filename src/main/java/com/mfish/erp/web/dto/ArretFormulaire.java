package com.mfish.erp.web.dto;

import com.mfish.erp.tournee.StatutArret;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Formulaire de mise a jour d'un arret de tournee (statut + montant encaisse).
 */
public record ArretFormulaire(
        @NotNull(message = "validation.obligatoire") StatutArret statut,
        @PositiveOrZero(message = "validation.positifOuZero") long montant) {
}
