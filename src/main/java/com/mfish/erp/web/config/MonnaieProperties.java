package com.mfish.erp.web.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration de l'affichage monetaire de l'interface web.
 *
 * @param devise libelle de la devise affichee apres les montants (ex. FCFA)
 */
@Validated
@ConfigurationProperties(prefix = "app.monnaie")
public record MonnaieProperties(@NotBlank String devise) {
}
