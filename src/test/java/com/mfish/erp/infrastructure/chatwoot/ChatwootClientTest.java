package com.mfish.erp.infrastructure.chatwoot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mfish.erp.config.ChatwootProperties;
import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.conversation.RegistreMessagesEmis;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ChatwootClientTest {

    private WireMockServer server;
    private RegistreMessagesEmis registre;
    private ChatwootClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        final ChatwootProperties properties = new ChatwootProperties(server.baseUrl(), "token", "1",
                "secret", Duration.ofSeconds(3), Duration.ofSeconds(5),
                new RetryProperties(3, Duration.ofMillis(10), 2.0, Duration.ofMillis(20)));
        registre = new RegistreMessagesEmis(
                Clock.fixed(Instant.parse("2026-11-19T00:00:00Z"), ZoneOffset.UTC));
        client = new ChatwootClient(RestClient.builder(), properties, new RetrySupport(), registre);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    @DisplayName("doit_envoyer_un_message")
    void doit_envoyer_un_message() {
        server.stubFor(post(urlPathEqualTo("/api/v1/accounts/1/conversations/conv-1/messages"))
                .willReturn(aResponse().withStatus(200)));

        client.envoyerMessage("conv-1", "bonjour");

        server.verify(postRequestedFor(urlPathEqualTo("/api/v1/accounts/1/conversations/conv-1/messages")));
        assertThat(registre.consommerEcho("conv-1", "bonjour"))
                .as("le message emis doit etre memorise pour ignorer son echo webhook")
                .isTrue();
    }

    @Test
    @DisplayName("doit_etiqueter_une_conversation")
    void doit_etiqueter_une_conversation() {
        server.stubFor(post(urlPathEqualTo("/api/v1/accounts/1/conversations/conv-1/labels"))
                .willReturn(aResponse().withStatus(200)));

        client.etiqueterConversation("conv-1", "reprendre-main");

        server.verify(postRequestedFor(urlPathEqualTo("/api/v1/accounts/1/conversations/conv-1/labels")));
    }

    @Test
    @DisplayName("doit_echouer_sur_erreur_serveur")
    void doit_echouer_sur_erreur_serveur() {
        server.stubFor(post(urlPathEqualTo("/api/v1/accounts/1/conversations/conv-1/messages"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.envoyerMessage("conv-1", "bonjour"))
                .isInstanceOf(RuntimeException.class);
        assertThat(registre.consommerEcho("conv-1", "bonjour"))
                .as("un envoi echoue ne doit laisser aucune entree residuelle")
                .isFalse();
    }
}
