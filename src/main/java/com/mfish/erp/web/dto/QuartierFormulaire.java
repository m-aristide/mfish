package com.mfish.erp.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Formulaire de creation/modification d'un quartier.
 *
 * <p>La latitude et la longitude sont optionnelles ; lorsqu'elles sont fournies
 * elles doivent rester dans les bornes geographiques.
 */
public record QuartierFormulaire(
        @NotBlank(message = "validation.obligatoire") String nom,
        String zone,
        @DecimalMin(value = "-90", message = "validation.latitude")
        @DecimalMax(value = "90", message = "validation.latitude") Double latitude,
        @DecimalMin(value = "-180", message = "validation.longitude")
        @DecimalMax(value = "180", message = "validation.longitude") Double longitude,
        @PositiveOrZero(message = "validation.positifOuZero") int ordreDefaut,
        Boolean actif) {
}
