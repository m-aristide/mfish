package com.mfish.erp.whatsapp;

import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.whatsapp.config.WhatsAppProperties;
import java.time.Duration;

/**
 * Fabrique de proprietes WhatsApp de test (aucune donnee reelle).
 */
public final class WhatsAppTestFactory {

    private WhatsAppTestFactory() {
    }

    public static WhatsAppProperties properties(final String appSecret) {
        return new WhatsAppProperties(
                true,
                "test-token",
                "123456789",
                "987654321",
                appSecret,
                "test-verify-token",
                "v21.0",
                "http://localhost:0",
                "+2250700000001",
                1048576,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                new RetryProperties(2, Duration.ofMillis(1), 2.0, Duration.ofMillis(2)));
    }
}
