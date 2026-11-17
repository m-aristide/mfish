package com.mfish.erp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RetrySupportTest {

    private final RetrySupport retrySupport = new RetrySupport();
    private final RetryProperties properties =
            new RetryProperties(3, Duration.ofMillis(5), 2.0, Duration.ofMillis(20));

    @Test
    @DisplayName("doit_retenter_puis_reussir")
    void doit_retenter_puis_reussir() {
        final AtomicInteger essais = new AtomicInteger();

        final String resultat = retrySupport.executer(properties, () -> {
            if (essais.incrementAndGet() < 3) {
                throw new RuntimeException("transitoire");
            }
            return "ok";
        });

        assertThat(resultat).isEqualTo("ok");
        assertThat(essais.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("doit_echouer_apres_epuisement_des_tentatives")
    void doit_echouer_apres_epuisement_des_tentatives() {
        assertThatThrownBy(() -> retrySupport.executer(properties, () -> {
            throw new RuntimeException("toujours");
        })).isInstanceOf(RuntimeException.class);
    }
}
