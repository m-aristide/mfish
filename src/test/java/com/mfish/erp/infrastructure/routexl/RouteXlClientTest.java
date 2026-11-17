package com.mfish.erp.infrastructure.routexl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mfish.erp.commun.AppelExterneException;
import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.config.RoutexlProperties;
import com.mfish.erp.tournee.ArretAOptimiser;
import com.mfish.erp.tournee.ArretPlanifie;
import com.mfish.erp.tournee.Coordonnees;
import com.mfish.erp.tournee.TourneeRoutingInput;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RouteXlClientTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private WireMockServer server;
    private RouteXlClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        final RoutexlProperties properties = new RoutexlProperties(server.baseUrl(), "user", "pass",
                20, Duration.ofSeconds(3), Duration.ofSeconds(5),
                new RetryProperties(3, Duration.ofMillis(10), 2.0, Duration.ofMillis(20)));
        client = new RouteXlClient(RestClient.builder(), properties, new RetrySupport());
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private TourneeRoutingInput input() {
        return new TourneeRoutingInput(new Coordonnees(5.32, -4.01), LocalTime.of(8, 0),
                List.of(new ArretAOptimiser(ID, new Coordonnees(5.35, -3.98), 0)));
    }

    @Test
    @DisplayName("doit_retourner_les_arrets_optimises_sur_succes")
    void doit_retourner_les_arrets_optimises_sur_succes() {
        server.stubFor(post(urlEqualTo("/tour"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"route\":{\"visits\":[{\"id\":\"" + ID + "\",\"arrival\":\"08:30\"}]}}")));

        final List<ArretPlanifie> resultat = client.optimiser(input());

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).commandeId()).isEqualTo(ID);
        assertThat(resultat.get(0).heureArrivee()).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    @DisplayName("doit_echouer_sur_erreur_serveur")
    void doit_echouer_sur_erreur_serveur() {
        server.stubFor(post(urlEqualTo("/tour")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.optimiser(input()))
                .isInstanceOf(AppelExterneException.class);
    }

    @Test
    @DisplayName("doit_echouer_sur_reponse_malformee")
    void doit_echouer_sur_reponse_malformee() {
        server.stubFor(post(urlEqualTo("/tour")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("pas-du-json")));

        assertThatThrownBy(() -> client.optimiser(input()))
                .isInstanceOf(RuntimeException.class);
    }
}
