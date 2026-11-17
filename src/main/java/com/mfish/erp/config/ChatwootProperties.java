package com.mfish.erp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration du client Chatwoot (boite partagee WhatsApp).
 * Les identifiants peuvent etre vides en developpement : l'adaptateur refuse alors
 * les appels avec une exception typee explicite.
 */
@Validated
@ConfigurationProperties(prefix = "app.chatwoot")
public record ChatwootProperties(
        String baseUrl,
        String apiToken,
        String accountId,
        String webhookSecret,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Valid @NotNull RetryProperties retry) {
}
