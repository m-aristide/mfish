package com.mfish.erp.securite;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneId.of("UTC"));
    private final LoginRateLimiter rateLimiter = new LoginRateLimiter(clock);

    @Test
    @DisplayName("doit_autoriser_cinq_tentatives")
    void doit_autoriser_cinq_tentatives() {
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.autorise("user")).isTrue();
        }
    }

    @Test
    @DisplayName("doit_bloquer_la_sixieme_tentative")
    void doit_bloquer_la_sixieme_tentative() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.autorise("user");
        }

        assertThat(rateLimiter.autorise("user")).isFalse();
    }
}
