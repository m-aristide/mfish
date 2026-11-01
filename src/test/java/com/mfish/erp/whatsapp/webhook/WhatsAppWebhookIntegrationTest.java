package com.mfish.erp.whatsapp.webhook;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.conversation.Conversation;
import com.mfish.erp.conversation.ConversationRepository;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.ia.OrchestrateurIa;
import com.mfish.erp.ia.ResultatIa;
import com.mfish.erp.whatsapp.client.WhatsAppPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "test-whatsapp"})
@ExtendWith(OutputCaptureExtension.class)
class WhatsAppWebhookIntegrationTest {

    private static final String APP_SECRET = "test-app-secret";
    private static final String VERIFY_TOKEN = "test-verify-token";
    private static final String ACCESS_TOKEN = "test-whatsapp-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WhatsAppMessageRepository messageRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @MockitoBean
    private WhatsAppPort whatsAppPort;

    @MockitoBean
    private OrchestrateurIa orchestrateurIa;

    @BeforeEach
    void nettoyer() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        reset(whatsAppPort, orchestrateurIa);
    }

    @Test
    @DisplayName("doit_valider_la_verification_get")
    void doit_valider_la_verification_get() throws Exception {
        mockMvc.perform(get("/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", VERIFY_TOKEN)
                        .param("hub.challenge", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("12345"));
    }

    @Test
    @DisplayName("doit_refuser_la_verification_avec_mauvais_token")
    void doit_refuser_la_verification_avec_mauvais_token() throws Exception {
        mockMvc.perform(get("/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "mauvais")
                        .param("hub.challenge", "12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("doit_refuser_un_post_sans_signature")
    void doit_refuser_un_post_sans_signature() throws Exception {
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lire("text.json")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("doit_refuser_un_post_avec_signature_invalide")
    void doit_refuser_un_post_avec_signature_invalide() throws Exception {
        final byte[] corps = lire("text.json");
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=00000000000000000000000000000000")
                        .content(corps))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("doit_refuser_un_payload_malforme")
    void doit_refuser_un_payload_malforme() throws Exception {
        final byte[] corps = "pas-du-json".getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=" + hmac(corps))
                        .content(corps))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("doit_traiter_un_message_valide_de_bout_en_bout")
    void doit_traiter_un_message_valide_de_bout_en_bout() throws Exception {
        when(orchestrateurIa.traiter(any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false));
        when(whatsAppPort.envoyerTexte(anyString(), anyString())).thenReturn("wamid.out.1");

        final byte[] corps = lire("text.json");
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=" + hmac(corps))
                        .content(corps))
                .andExpect(status().isOk());

        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(messageRepository.existsByWamid("wamid.text.123")).isTrue();
            assertThat(clientRepository.findByTelephone("+2250700000001")).isPresent();
            assertThat(conversationRepository.findByTelephone("+2250700000001")).isPresent();
            verify(whatsAppPort).envoyerTexte(eq("+2250700000001"), eq("ok"));
        });
    }

    @Test
    @DisplayName("doit_ignorer_un_doublon_de_wamid")
    void doit_ignorer_un_doublon_de_wamid() throws Exception {
        when(orchestrateurIa.traiter(any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false));
        when(whatsAppPort.envoyerTexte(anyString(), anyString())).thenReturn("wamid.out.1");

        final byte[] corps = lire("text.json");
        final String signature = "sha256=" + hmac(corps);
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", signature)
                        .content(corps))
                .andExpect(status().isOk());
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", signature)
                        .content(corps))
                .andExpect(status().isOk());

        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(whatsAppPort, times(1)).envoyerTexte(anyString(), anyString()));
    }

    @Test
    @DisplayName("doit_mettre_a_jour_le_statut_d_un_message_sortant")
    void doit_mettre_a_jour_le_statut_d_un_message_sortant() throws Exception {
        messageRepository.save(new WhatsAppMessage("wamid.out.123", DirectionMessage.SORTANT,
                "+2250700000001", TypeMessage.TEXTE, "bonjour", StatutMessage.ENVOYE, null, null));

        final byte[] corps = lire("status.json");
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=" + hmac(corps))
                        .content(corps))
                .andExpect(status().isOk());

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(messageRepository.findByWamid("wamid.out.123")).get()
                        .extracting(WhatsAppMessage::getStatut).isEqualTo(StatutMessage.LIVRE));
    }

    @Test
    @DisplayName("doit_rester_silencieux_en_mode_humain")
    void doit_rester_silencieux_en_mode_humain() throws Exception {
        final Client client = clientRepository.findByTelephone("+2250700000001").orElseThrow();
        final Conversation conversation = new Conversation(null, ModeConversation.HUMAIN);
        conversation.setTelephone("+2250700000001");
        conversation.setClient(client);
        conversationRepository.save(conversation);

        final byte[] corps = lire("text.json");
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=" + hmac(corps))
                        .content(corps))
                .andExpect(status().isOk());

        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(whatsAppPort).marquerLu(eq("+2250700000001"), eq("wamid.text.123")));
        verify(whatsAppPort, never()).envoyerTexte(anyString(), anyString());
    }

    @Test
    @DisplayName("doit_escalader_en_mode_humain_sur_panne_ia")
    void doit_escalader_en_mode_humain_sur_panne_ia() throws Exception {
        when(orchestrateurIa.traiter(any(), anyList(), anyString()))
                .thenThrow(new RuntimeException("panne IA"));

        final byte[] corps = lire("text.json");
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=" + hmac(corps))
                        .content(corps))
                .andExpect(status().isOk());

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(conversationRepository.findByTelephone("+2250700000001")).get()
                        .extracting(Conversation::getMode).isEqualTo(ModeConversation.HUMAIN));
    }

    @Test
    @DisplayName("ne_fuit_ni_jeton_ni_numero_dans_les_logs")
    void ne_fuit_ni_jeton_ni_numero_dans_les_logs(final CapturedOutput output) throws Exception {
        when(orchestrateurIa.traiter(any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false));
        when(whatsAppPort.envoyerTexte(anyString(), anyString())).thenReturn("wamid.out.1");

        final byte[] corps = lire("text.json");
        mockMvc.perform(post("/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=" + hmac(corps))
                        .content(corps))
                .andExpect(status().isOk());

        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(whatsAppPort).envoyerTexte(anyString(), anyString()));

        assertThat(output.getAll()).doesNotContain(ACCESS_TOKEN);
        assertThat(output.getAll()).doesNotContain("2250700000001");
    }

    @Test
    @DisplayName("le_reste_de_l_application_reste_protege")
    void le_reste_de_l_application_reste_protege() throws Exception {
        mockMvc.perform(get("/conversations")).andExpect(status().is3xxRedirection());
    }

    private byte[] lire(final String fichier) throws IOException {
        try (final var in = getClass().getClassLoader().getResourceAsStream("whatsapp/" + fichier)) {
            assertThat(in).as("fichier %s", fichier).isNotNull();
            return in.readAllBytes();
        }
    }

    private String hmac(final byte[] corps) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(corps));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

