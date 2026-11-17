package com.mfish.erp.simulateur;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mfish.erp.config.ChatwootProperties;
import com.mfish.erp.config.RetryProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulateurServiceTest {

    private static final String SECRET = "secret-simulateur";

    private WireMockServer server;
    private ChatwootProperties properties;
    private Clock clock;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        properties = new ChatwootProperties(
                server.baseUrl(), "token", "1", SECRET,
                Duration.ofSeconds(3), Duration.ofSeconds(5),
                new RetryProperties(3, Duration.ofMillis(10), 2.0, Duration.ofMillis(20)));
        clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneId.of("UTC"));
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private SimulateurService service(final String mode, final String inbox) {
        return new SimulateurService(properties, clock,
                new SimulateurMessageStore(), new SimulateurConversationStore(),
                mode, inbox, server.port());
    }

    @Test
    @DisplayName("envoie un webhook signe au format Chatwoot (mode LOCAL)")
    void envoie_un_webhook_signe() {
        server.stubFor(post(urlPathEqualTo("/webhooks/chatwoot"))
                .willReturn(aResponse().withStatus(200)));

        service("LOCAL", "").envoyer(
                new SimulateurMessageRequest("conv-1", "+237650000001", "Client", "Bonjour"));

        server.verify(postRequestedFor(urlPathEqualTo("/webhooks/chatwoot"))
                .withHeader("X-Chatwoot-Signature", matching("sha256=[0-9a-f]{64}"))
                .withHeader("X-Chatwoot-Timestamp", matching("\\d+"))
                .withRequestBody(matchingJsonPath("$.event", equalTo("message_created")))
                .withRequestBody(matchingJsonPath("$.message_type", equalTo("incoming")))
                .withRequestBody(matchingJsonPath("$.conversation.id", equalTo("conv-1")))
                .withRequestBody(matchingJsonPath("$.sender.phone_number", equalTo("+237650000001"))));
    }

    @Test
    @DisplayName("signe le corps avec le secret partage (HMAC-SHA256)")
    void signe_le_corps_avec_le_secret() {
        server.stubFor(post(urlPathEqualTo("/webhooks/chatwoot"))
                .willReturn(aResponse().withStatus(200)));

        service("LOCAL", "").envoyer(
                new SimulateurMessageRequest("conv-2", "+237650000002", "Client", "Commande"));

        final com.github.tomakehurst.wiremock.http.Request requete =
                server.getAllServeEvents().get(0).getRequest();
        final String corps = requete.getBodyAsString();
        final String signature = requete.getHeader("X-Chatwoot-Signature");
        final String horodatage = requete.getHeader("X-Chatwoot-Timestamp");

        assertThat(signature).isEqualTo("sha256=" + hmac(horodatage + "." + corps));
    }

    @Test
    @DisplayName("cree le contact, la conversation et envoie le message (mode CHATWOOT)")
    void cree_contact_conversation_et_message() {
        server.stubFor(post(urlPathEqualTo("/public/api/v1/inboxes/inbox-1/contacts"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"source_id\":\"src-1\"}")));
        server.stubFor(post(urlPathEqualTo("/public/api/v1/inboxes/inbox-1/contacts/src-1/conversations"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":42}")));
        server.stubFor(post(urlPathEqualTo("/public/api/v1/inboxes/inbox-1/contacts/src-1/conversations/42/messages"))
                .willReturn(aResponse().withStatus(200)));

        service("CHATWOOT", "inbox-1").envoyer(
                new SimulateurMessageRequest("sim-1", "+237650000001", "Client", "Bonjour"));

        server.verify(postRequestedFor(urlPathEqualTo("/public/api/v1/inboxes/inbox-1/contacts")));
        server.verify(postRequestedFor(
                urlPathEqualTo("/public/api/v1/inboxes/inbox-1/contacts/src-1/conversations")));
        server.verify(postRequestedFor(
                urlPathEqualTo("/public/api/v1/inboxes/inbox-1/contacts/src-1/conversations/42/messages")));
    }

    @Test
    @DisplayName("lit uniquement les reponses de l'IA depuis Chatwoot (mode CHATWOOT)")
    void lit_les_reponses_ia_de_chatwoot() {
        final SimulateurConversationStore conversationStore = new SimulateurConversationStore();
        conversationStore.enregistrer("sim-1", new SimulateurConversation("src-1", "42"));
        server.stubFor(get(urlPathEqualTo("/api/v1/accounts/1/conversations/42/messages"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"payload\":["
                                + "{\"content\":\"Bonjour\",\"message_type\":\"incoming\",\"created_at\":1700000000},"
                                + "{\"content\":\"Votre commande est enregistree\",\"message_type\":\"outgoing\",\"created_at\":1700000001}"
                                + "]}")));

        final SimulateurService service = new SimulateurService(properties, clock,
                new SimulateurMessageStore(), conversationStore, "CHATWOOT", "inbox-1", server.port());

        final List<SimulateurMessage> messages = service.listerMessages("sim-1");

        assertThat(messages).extracting(SimulateurMessage::contenu)
                .containsExactly("Votre commande est enregistree");
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
