package com.mfish.erp.quartier;

import jakarta.validation.constraints.NotBlank;

/**
 * Entree de creation/modification d'un quartier.
 */
public record QuartierRequest(
        @NotBlank String nom,
        String zone,
        Double latitude,
        Double longitude,
        int ordreDefaut,
        boolean actif) {
}
