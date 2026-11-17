package com.mfish.erp.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Question d'un client fictif pour la zone de test de l'IA.
 */
public record QuestionTestFormulaire(@NotBlank(message = "validation.obligatoire") String question) {
}
