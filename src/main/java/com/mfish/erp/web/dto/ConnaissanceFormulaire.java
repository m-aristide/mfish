package com.mfish.erp.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Formulaire d'enregistrement de la base de connaissance (contenu Markdown).
 */
public record ConnaissanceFormulaire(@NotBlank(message = "validation.obligatoire") String contenu) {
}
