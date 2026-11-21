package com.mfish.erp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration du client Google Geocoding (resolution des quartiers inconnus).
 *
 * @param baseUrl        racine de l'API (ex. {@code https://maps.googleapis.com})
 * @param apiKey         cle d'API Google (variable {@code GOOGLE_GEOCODING_API})
 * @param connectTimeout delai maximal d'etablissement de connexion
 * @param readTimeout    delai maximal de lecture de la reponse
 * @param retry          politique de retry partagee (backoff exponentiel)
 */
@Validated
@ConfigurationProperties(prefix = "app.geocoding")
public record GeocodingProperties(
        String baseUrl,
        String apiKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Valid @NotNull RetryProperties retry) {
}
