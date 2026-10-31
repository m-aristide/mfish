package com.mfish.erp.infrastructure.deepseek;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mfish.erp.commun.AppelExterneException;
import com.mfish.erp.config.DeepSeekProperties;
import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.config.RetrySupport;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DeepSeekClientTest {

    private WireMockServer server;
    private DeepSeekClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        final DeepSeekProperties properties = new DeepSeekProperties(server.baseUrl(), "test-key",
                "deepseek-chat", Duration.ofSeconds(3), Duration.ofSeconds(5),
                new RetryProperties(3, Duration.ofMillis(10), 2.0, Duration.ofMillis(20)));
        client = new DeepSeekClient(RestClient.builder(), properties, new RetrySupport());
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    @DisplayName("doit_retourner_le_contenu_sur_succes")
    void doit_retourner_le_contenu_sur_succes() {
        server.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"reponse\\\":\\\"ok\\\"}\"}}]}")));

        final String contenu = client.generer("prompt", List.of("bonjour"));

        assertThat(contenu).contains("reponse");
    }

    @Test
    @DisplayName("doit_echouer_sur_erreur_serveur")
    void doit_echouer_sur_erreur_serveur() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.generer("prompt", List.of("bonjour")))
                .isInstanceOf(AppelExterneException.class);
    }
}
