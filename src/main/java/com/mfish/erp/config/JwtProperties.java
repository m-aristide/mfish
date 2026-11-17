package com.mfish.erp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration des jetons JWT signes en HMAC-SHA256.
 *
 * @param secret   cle secrete partagee (32 octets minimum, voir .env.example)
 * @param validity duree de validite d'un jeton emis
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration validity) {
}
