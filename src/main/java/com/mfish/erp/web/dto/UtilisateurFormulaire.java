package com.mfish.erp.web.dto;

import com.mfish.erp.utilisateur.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Formulaire de creation d'un utilisateur (role + mot de passe requis a la creation).
 */
public record UtilisateurFormulaire(
        @NotBlank(message = "validation.obligatoire") String username,
        @NotBlank(message = "validation.obligatoire") String motDePasse,
        @NotNull(message = "validation.obligatoire") Role role) {
}
