package com.mfish.erp.whatsapp.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.whatsapp.config.WhatsAppProperties;
import com.mfish.erp.whatsapp.metrics.WhatsAppMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class WhatsAppClientTest {

    private static final String CHEMIN = "/v21.0/123456789/messages";

    private WireMockServer server;
    private WhatsAppClient client;

    @BeforeEach
    void demarrer() {
        server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();
        client = client(Duration.ofSeconds(2));
    }

    @AfterEach
    void arreter() {
        server.stop();
    }

    @Test
    @DisplayName("doit_envoyer_un_texte_et_retourner_le_wamid")
    void doit_envoyer_un_texte_et_retourner_le_wamid() {
        server.stubFor(post(urlEqualTo(CHEMIN))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messages\":[{\"id\":\"wamid.1\"}]}")));

        assertThat(client.envoyerTexte("+2250700000001", "bonjour")).isEqualTo("wamid.1");
    }

    @Test
    @DisplayName("doit_traduire_une_erreur_401_en_jeton_invalide_sans_retry")
    void doit_traduire_une_erreur_401_en_jeton_invalide_sans_retry() {
        server.stubFor(post(urlEqualTo(CHEMIN))
                .willReturn(aResponse().withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"expired\",\"code\":190}}")));

        assertThatThrownBy(() -> client.envoyerTexte("+2250700000001", "bonjour"))
                .isInstanceOf(WhatsAppTokenInvalideException.class);
        server.verify(1, postRequestedFor(urlEqualTo(CHEMIN)));
    }

    @Test
    @DisplayName("doit_traduire_une_erreur_400_en_hors_fenetre_sans_retry")
    void doit_traduire_une_erreur_400_en_hors_fenetre_sans_retry() {
        server.stubFor(post(urlEqualTo(CHEMIN))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"re-engagement\",\"code\":131047}}")));

        assertThatThrownBy(() -> client.envoyerTexte("+2250700000001", "bonjour"))
                .isInstanceOf(WhatsAppHorsFenetreException.class);
        server.verify(1, postRequestedFor(urlEqualTo(CHEMIN)));
    }

    @Test
    @DisplayName("doit_retenter_sur_429_puis_reussir")
    void doit_retenter_sur_429_puis_reussir() {
        server.stubFor(post(urlEqualTo(CHEMIN))
                .inScenario("retry429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0"))
                .willSetStateTo("second"));
        server.stubFor(post(urlEqualTo(CHEMIN))
                .inScenario("retry429")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messages\":[{\"id\":\"wamid.429\"}]}")));

        assertThat(client.envoyerTexte("+2250700000001", "bonjour")).isEqualTo("wamid.429");
        server.verify(2, postRequestedFor(urlEqualTo(CHEMIN)));
    }

    @Test
    @DisplayName("doit_retenter_sur_5xx_puis_reussir")
    void doit_retenter_sur_5xx_puis_reussir() {
        server.stubFor(post(urlEqualTo(CHEMIN))
                .inScenario("retry5xx")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));
        server.stubFor(post(urlEqualTo(CHEMIN))
                .inScenario("retry5xx")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messages\":[{\"id\":\"wamid.500\"}]}")));

        assertThat(client.envoyerTexte("+2250700000001", "bonjour")).isEqualTo("wamid.500");
        server.verify(2, postRequestedFor(urlEqualTo(CHEMIN)));
    }

    @Test
    @DisplayName("doit_abandonner_apres_epuisement_des_tentatives_sur_5xx")
    void doit_abandonner_apres_epuisement_des_tentatives_sur_5xx() {
        server.stubFor(post(urlEqualTo(CHEMIN)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.envoyerTexte("+2250700000001", "bonjour"))
                .isInstanceOf(WhatsAppException.class);
        server.verify(2, postRequestedFor(urlEqualTo(CHEMIN)));
    }

    @Test
    @DisplayName("doit_lever_une_exception_sur_timeout")
    void doit_lever_une_exception_sur_timeout() {
        final WhatsAppClient clientTimeout = client(Duration.ofMillis(200));
        server.stubFor(post(urlEqualTo(CHEMIN))
                .willReturn(aResponse().withStatus(200).withFixedDelay(1500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messages\":[{\"id\":\"tard\"}]}")));

        assertThatThrownBy(() -> clientTimeout.envoyerTexte("+2250700000001", "bonjour"))
                .isInstanceOf(WhatsAppException.class);
    }

    @Test
    @DisplayName("doit_envoyer_un_template_avec_variables")
    void doit_envoyer_un_template_avec_variables() {
        server.stubFor(post(urlEqualTo(CHEMIN))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messages\":[{\"id\":\"wamid.tpl\"}]}")));

        assertThat(client.envoyerTemplate("+2250700000001", "confirmation_commande", "fr",
                List.of("123", "18h"))).isEqualTo("wamid.tpl");
    }

    private WhatsAppClient client(final Duration readTimeout) {
        final WhatsAppProperties props = new WhatsAppProperties(
                true, "test-token", "123456789", "987654321", "test-app-secret",
                "test-verify-token", "v21.0", "http://localhost:" + server.port(),
                "+2250700000001", 1048576, Duration.ofSeconds(1), readTimeout,
                new RetryProperties(2, Duration.ofMillis(1), 2.0, Duration.ofMillis(2)));
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(1));
        requestFactory.setReadTimeout(readTimeout);
        final RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + server.port())
                .requestFactory(requestFactory)
                .build();
        final WhatsAppMetrics metrics = new WhatsAppMetrics(new SimpleMeterRegistry());
        return new WhatsAppClient(restClient, props, new WhatsAppErreurTraducteur(), metrics);
    }
}
