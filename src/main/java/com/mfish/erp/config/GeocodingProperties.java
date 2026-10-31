package com.mfish.erp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration du fournisseur de geocodage (adresse/quartier vers coordonnees GPS).
 */
@Validated
@ConfigurationProperties(prefix = "app.geocoding")
public record GeocodingProperties(
        String provider,
        String baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Valid @NotNull RetryProperties retry) {
}
