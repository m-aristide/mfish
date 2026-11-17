package com.mfish.erp.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * Formulaire de creation/modification d'un client (jamais d'entite JPA liee).
 */
public record ClientFormulaire(
        @NotBlank(message = "validation.obligatoire") String nom,
        @NotBlank(message = "validation.obligatoire")
        @Pattern(regexp = "\\+[1-9][0-9]{7,14}", message = "validation.telephone")
        String telephone,
        UUID quartierId,
        String adresse,
        String repere,
        Double latitude,
        Double longitude,
        String notes) {
}
