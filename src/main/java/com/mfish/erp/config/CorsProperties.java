package com.mfish.erp.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Origines autorisees pour le CORS (la PWA consomme l'API depuis une autre origine).
 *
 * @param allowedOrigins liste non vide d'origines autorisees
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @NotEmpty List<String> allowedOrigins) {
}
