package com.mfish.erp.infrastructure.geocodage;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mfish.erp.config.GeocodingProperties;
import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.tournee.Coordonnees;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NominatimGeocodingClientTest {

    private WireMockServer server;
    private NominatimGeocodingClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        final GeocodingProperties properties = new GeocodingProperties("nominatim", server.baseUrl(),
                Duration.ofSeconds(3), Duration.ofSeconds(5),
                new RetryProperties(3, Duration.ofMillis(10), 2.0, Duration.ofMillis(20)));
        client = new NominatimGeocodingClient(RestClient.builder(), properties, new RetrySupport());
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    @DisplayName("doit_retourner_des_coordonnees_sur_succes")
    void doit_retourner_des_coordonnees_sur_succes() {
        server.stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("Cocody"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"lat\":\"5.35\",\"lon\":\"-3.97\"}]")));

        assertThat(client.geocoder("Cocody")).contains(new Coordonnees(5.35, -3.97));
    }

    @Test
    @DisplayName("doit_retourner_vide_si_aucun_resultat")
    void doit_retourner_vide_si_aucun_resultat() {
        server.stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        assertThat(client.geocoder("inconnu")).isEmpty();
    }
}
