package com.mfish.erp.whatsapp.health;

import com.mfish.erp.whatsapp.config.WhatsAppProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicateur de sante WhatsApp : signale si la configuration est valide, sans appeler Meta.
 * En mode desactive, l'indicateur reste UP (detail "desactive").
 */
@Component
public class WhatsAppHealthIndicator implements HealthIndicator {

    private final WhatsAppProperties properties;

    public WhatsAppHealthIndicator(final WhatsAppProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.enabled()) {
            return Health.up().withDetail("whatsapp", "desactive").build();
        }
        if (properties.isConfigurationComplete()) {
            return Health.up().withDetail("whatsapp", "configure").build();
        }
        return Health.down().withDetail("whatsapp", "configuration incomplète").build();
    }
}
