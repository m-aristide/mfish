package com.mfish.erp.whatsapp.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.whatsapp.config.WhatsAppProperties;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhatsAppHealthIndicatorTest {

    @Test
    @DisplayName("doit_etre_up_quand_le_module_est_desactive")
    void doit_etre_up_quand_le_module_est_desactive() {
        final WhatsAppHealthIndicator indicateur = new WhatsAppHealthIndicator(properties(false));

        assertThat(indicateur.health().getStatus().getCode()).isEqualTo("UP");
        assertThat(indicateur.health().getDetails()).containsEntry("whatsapp", "desactive");
    }

    @Test
    @DisplayName("doit_etre_up_quand_la_configuration_est_complete")
    void doit_etre_up_quand_la_configuration_est_complete() {
        final WhatsAppHealthIndicator indicateur = new WhatsAppHealthIndicator(properties(true));

        assertThat(indicateur.health().getStatus().getCode()).isEqualTo("UP");
        assertThat(indicateur.health().getDetails()).containsEntry("whatsapp", "configure");
    }

    @Test
    @DisplayName("doit_etre_down_quand_la_configuration_est_incomplete")
    void doit_etre_down_quand_la_configuration_est_incomplete() {
        final WhatsAppProperties props = new WhatsAppProperties(
                true, null, "123", null, null, null, "v21.0", "http://localhost",
                null, 1024, Duration.ofSeconds(1), Duration.ofSeconds(2), retry());
        final WhatsAppHealthIndicator indicateur = new WhatsAppHealthIndicator(props);

        assertThat(indicateur.health().getStatus().getCode()).isEqualTo("DOWN");
    }

    private WhatsAppProperties properties(final boolean enabled) {
        return new WhatsAppProperties(
                enabled, "token", "123", "456", "secret", "verify", "v21.0",
                "http://localhost", "+2250700000001", 1024,
                Duration.ofSeconds(1), Duration.ofSeconds(2), retry());
    }

    private RetryProperties retry() {
        return new RetryProperties(2, Duration.ofMillis(1), 2.0, Duration.ofMillis(2));
    }
}
