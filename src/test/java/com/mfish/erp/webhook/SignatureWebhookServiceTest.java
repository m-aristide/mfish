package com.mfish.erp.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.config.ChatwootProperties;
import com.mfish.erp.config.RetryProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignatureWebhookServiceTest {

    private static final String SECRET = "secret-partage";

    private final SignatureWebhookService service = new SignatureWebhookService(
            new ChatwootProperties("http://localhost", "token", "1", SECRET,
                    Duration.ofSeconds(3), Duration.ofSeconds(5),
                    new RetryProperties(3, Duration.ofMillis(10), 2.0, Duration.ofMillis(20))));

    @Test
    @DisplayName("doit_accepter_une_signature_valide")
    void doit_accepter_une_signature_valide() {
        final String corps = "{\"event\":\"message_created\"}";

        assertThat(service.estValide(corps, hmac(corps))).isTrue();
    }

    @Test
    @DisplayName("doit_refuser_une_signature_invalide")
    void doit_refuser_une_signature_invalide() {
        assertThat(service.estValide("corps", "mauvaise-signature")).isFalse();
    }

    @Test
    @DisplayName("doit_refuser_une_signature_absente")
    void doit_refuser_une_signature_absente() {
        assertThat(service.estValide("corps", null)).isFalse();
    }

    private String hmac(final String corps) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(corps.getBytes(StandardCharsets.UTF_8)));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
