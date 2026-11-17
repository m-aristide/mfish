package com.mfish.erp.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.config.ChatwootProperties;
import com.mfish.erp.config.RetryProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignatureWebhookServiceTest {

    private static final String SECRET = "secret-partage";
    private static final Instant MAINTENANT = Instant.parse("2026-11-09T10:00:00Z");
    private static final String HORODATAGE = String.valueOf(MAINTENANT.getEpochSecond());

    private final SignatureWebhookService service = new SignatureWebhookService(
            new ChatwootProperties("http://localhost", "token", "1", SECRET,
                    Duration.ofSeconds(3), Duration.ofSeconds(5),
                    new RetryProperties(3, Duration.ofMillis(10), 2.0, Duration.ofMillis(20))),
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    @Test
    @DisplayName("doit_accepter_une_signature_chatwoot_avec_horodatage")
    void doit_accepter_une_signature_chatwoot_avec_horodatage() {
        final String corps = "{\"event\":\"message_created\"}";

        assertThat(service.estValide(corps, signatureChatwoot(corps, HORODATAGE), HORODATAGE))
                .isTrue();
    }

    @Test
    @DisplayName("doit_refuser_une_signature_chatwoot_invalide")
    void doit_refuser_une_signature_chatwoot_invalide() {
        final String corps = "{\"event\":\"message_created\"}";

        assertThat(service.estValide(corps, "sha256=mauvaise-signature", HORODATAGE)).isFalse();
    }

    @Test
    @DisplayName("doit_refuser_un_horodatage_expire")
    void doit_refuser_un_horodatage_expire() {
        final String corps = "{\"event\":\"message_created\"}";
        final String ancien = String.valueOf(MAINTENANT.minus(Duration.ofMinutes(10)).getEpochSecond());

        assertThat(service.estValide(corps, signatureChatwoot(corps, ancien), ancien)).isFalse();
    }

    @Test
    @DisplayName("doit_refuser_un_horodatage_non_numerique")
    void doit_refuser_un_horodatage_non_numerique() {
        final String corps = "{\"event\":\"message_created\"}";

        assertThat(service.estValide(corps, signatureChatwoot(corps, "abc"), "abc")).isFalse();
    }

    @Test
    @DisplayName("doit_accepter_une_signature_valide_sans_horodatage")
    void doit_accepter_une_signature_valide_sans_horodatage() {
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

    private String signatureChatwoot(final String corps, final String horodatage) {
        return "sha256=" + hmac(horodatage + "." + corps);
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
