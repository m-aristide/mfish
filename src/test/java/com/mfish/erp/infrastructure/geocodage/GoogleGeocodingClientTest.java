package com.mfish.erp.infrastructure.geocodage;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mfish.erp.commun.AppelExterneException;
import com.mfish.erp.config.GeocodingProperties;
import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.tournee.Coordonnees;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GoogleGeocodingClientTest {

    private WireMockServer server;
    private GoogleGeocodingClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        final GeocodingProperties properties = new GeocodingProperties(server.baseUrl(), "test-key",
                Duration.ofSeconds(3), Duration.ofSeconds(5),
                new RetryProperties(1, Duration.ofMillis(10), 2.0, Duration.ofMillis(20)));
        client = new GoogleGeocodingClient(RestClient.builder(), properties, new RetrySupport());
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    @DisplayName("doit_retourner_les_coordonnees_sur_succes")
    void doit_retourner_les_coordonnees_sur_succes() {
        server.stubFor(get(urlPathEqualTo("/maps/api/geocode/json"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"OK\",\"results\":[{\"geometry\":{\"location\":"
                                + "{\"lat\":3.8667,\"lng\":11.5167}}}]}")));

        final Optional<Coordonnees> resultat = client.geocoder("Bastos, Yaounde, Cameroun");

        assertThat(resultat).isPresent();
        assertThat(resultat.get().latitude()).isEqualTo(3.8667);
        assertThat(resultat.get().longitude()).isEqualTo(11.5167);
    }

    @Test
    @DisplayName("doit_retourner_vide_quand_aucun_resultat")
    void doit_retourner_vide_quand_aucun_resultat() {
        server.stubFor(get(urlPathEqualTo("/maps/api/geocode/json"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ZERO_RESULTS\",\"results\":[]}")));

        assertThat(client.geocoder("Quartier fantome")).isEmpty();
    }

    @Test
    @DisplayName("doit_echouer_sur_erreur_serveur")
    void doit_echouer_sur_erreur_serveur() {
        server.stubFor(get(urlPathEqualTo("/maps/api/geocode/json"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.geocoder("Bastos, Yaounde, Cameroun"))
                .isInstanceOf(AppelExterneException.class);
    }

    @Test
    @DisplayName("doit_echouer_sur_statut_refuse")
    void doit_echouer_sur_statut_refuse() {
        server.stubFor(get(urlPathEqualTo("/maps/api/geocode/json"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"REQUEST_DENIED\",\"results\":[]}")));

        assertThatThrownBy(() -> client.geocoder("Bastos, Yaounde, Cameroun"))
                .isInstanceOf(AppelExterneException.class);
    }
}
