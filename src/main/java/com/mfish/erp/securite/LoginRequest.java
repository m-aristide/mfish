package com.mfish.erp.securite;

import jakarta.validation.constraints.NotBlank;

/**
 * Requete de connexion.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
