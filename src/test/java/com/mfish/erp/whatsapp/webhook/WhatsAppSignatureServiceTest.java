package com.mfish.erp.whatsapp.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.whatsapp.WhatsAppTestFactory;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhatsAppSignatureServiceTest {

    private static final String SECRET = "test-app-secret";

    private final WhatsAppSignatureService service =
            new WhatsAppSignatureService(WhatsAppTestFactory.properties(SECRET));

    @Test
    @DisplayName("doit_accepter_une_signature_valide")
    void doit_accepter_une_signature_valide() {
        final byte[] corps = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(service.estValide(corps, "sha256=" + hmac(corps))).isTrue();
    }

    @Test
    @DisplayName("doit_refuser_une_signature_invalide")
    void doit_refuser_une_signature_invalide() {
        final byte[] corps = "corps".getBytes(StandardCharsets.UTF_8);

        assertThat(service.estValide(corps, "sha256=0123456789abcdef")).isFalse();
    }

    @Test
    @DisplayName("doit_refuser_une_signature_absente")
    void doit_refuser_une_signature_absente() {
        assertThat(service.estValide("corps".getBytes(StandardCharsets.UTF_8), null)).isFalse();
    }

    @Test
    @DisplayName("doit_refuser_un_corps_modifie")
    void doit_refuser_un_corps_modifie() {
        final byte[] corps = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        final String signature = "sha256=" + hmac(corps);

        assertThat(service.estValide("{\"a\":2}".getBytes(StandardCharsets.UTF_8), signature)).isFalse();
    }

    @Test
    @DisplayName("doit_refuser_un_prefixe_incorrect")
    void doit_refuser_un_prefixe_incorrect() {
        final byte[] corps = "corps".getBytes(StandardCharsets.UTF_8);

        assertThat(service.estValide(corps, hmac(corps))).isFalse();
    }

    private String hmac(final byte[] corps) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(corps));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
