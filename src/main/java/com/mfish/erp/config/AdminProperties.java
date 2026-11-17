package com.mfish.erp.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Compte administrateur de l'interface de gestion.
 *
 * @param username     identifiant de connexion
 * @param passwordHash hash du mot de passe au format Spring Security ({@code {bcrypt}...} ou
 *                     {@code {noop}...} en developpement uniquement)
 */
@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotBlank String username,
        @NotBlank String passwordHash) {
}
