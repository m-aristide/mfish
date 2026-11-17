package com.mfish.erp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration du client RouteXL (optimisation de tournee).
 */
@Validated
@ConfigurationProperties(prefix = "app.routexl")
public record RoutexlProperties(
        String baseUrl,
        String username,
        String password,
        @Positive int maxStops,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Valid @NotNull RetryProperties retry) {
}
