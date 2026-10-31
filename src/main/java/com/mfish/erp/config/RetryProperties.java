package com.mfish.erp.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * Politique de retry avec backoff exponentiel, partagee par tous les adaptateurs
 * d'infrastructure (Chatwoot, DeepSeek, RouteXL, geocodage).
 *
 * @param maxAttempts  nombre total de tentatives (>= 1)
 * @param initialDelay delai avant la premiere retentative
 * @param multiplier   facteur multiplicatif du delai entre deux tentatives
 * @param maxDelay     borne superieure du delai entre deux tentatives
 */
public record RetryProperties(
        @Min(1) int maxAttempts,
        @NotNull Duration initialDelay,
        @Positive double multiplier,
        @NotNull Duration maxDelay) {
}
