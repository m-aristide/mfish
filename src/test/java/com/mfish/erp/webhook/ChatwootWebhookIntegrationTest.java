package com.mfish.erp.webhook;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mfish.erp.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ChatwootWebhookIntegrationTest {

    private static final String SECRET = "test-webhook-secret";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("doit_refuser_une_requete_sans_signature")
    void doit_refuser_une_requete_sans_signature() throws Exception {
        mockMvc.perform(post("/webhooks/chatwoot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"message_created\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("doit_refuser_un_payload_malforme")
    void doit_refuser_un_payload_malforme() throws Exception {
        final String corps = "pas-du-json";
        final String horodatage = horodatage();
        mockMvc.perform(post("/webhooks/chatwoot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Chatwoot-Signature", signatureChatwoot(corps, horodatage))
                        .header("X-Chatwoot-Timestamp", horodatage)
                        .content(corps))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("doit_accepter_un_evenement_valide")
    void doit_accepter_un_evenement_valide() throws Exception {
        final String corps = "{\"event\":\"conversation_created\",\"id\":\"evt-1\"}";
        final String horodatage = horodatage();
        mockMvc.perform(post("/webhooks/chatwoot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Chatwoot-Signature", signatureChatwoot(corps, horodatage))
                        .header("X-Chatwoot-Timestamp", horodatage)
                        .content(corps))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("doit_refuser_un_horodatage_expire")
    void doit_refuser_un_horodatage_expire() throws Exception {
        final String corps = "{\"event\":\"conversation_created\",\"id\":\"evt-2\"}";
        final String horodatage = String.valueOf(Instant.now().minusSeconds(900).getEpochSecond());
        mockMvc.perform(post("/webhooks/chatwoot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Chatwoot-Signature", signatureChatwoot(corps, horodatage))
                        .header("X-Chatwoot-Timestamp", horodatage)
                        .content(corps))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("doit_accepter_le_format_historique_sans_horodatage")
    void doit_accepter_le_format_historique_sans_horodatage() throws Exception {
        final String corps = "{\"event\":\"conversation_created\",\"id\":\"evt-3\"}";
        mockMvc.perform(post("/webhooks/chatwoot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Chatwoot-Signature", hmac(corps))
                        .content(corps))
                .andExpect(status().isOk());
    }

    private String horodatage() {
        return String.valueOf(Instant.now().getEpochSecond());
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
