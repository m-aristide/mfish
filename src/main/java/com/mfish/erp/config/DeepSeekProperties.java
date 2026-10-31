package com.mfish.erp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration du client DeepSeek (module IA).
 */
@Validated
@ConfigurationProperties(prefix = "app.deepseek")
public record DeepSeekProperties(
        String baseUrl,
        String apiKey,
        String model,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Valid @NotNull RetryProperties retry) {
}
