package com.mfish.erp.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * Entree de creation/modification d'un client.
 */
public record ClientRequest(
        @NotBlank String nom,
        @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}", message = "telephone au format E.164") String telephone,
        UUID quartierId,
        String adresse,
        String repere,
        Double latitude,
        Double longitude,
        String notes) {
}
