package com.mfish.erp.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Regle24HeuresTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneId.of("UTC"));

    @Test
    @DisplayName("doit_autoriser_un_message_libre_dans_les_24_heures")
    void doit_autoriser_un_message_libre_dans_les_24_heures() {
        final Instant derniereActivite = clock.instant().minus(Duration.ofHours(23));

        assertThat(Regle24Heures.peutEnvoyerMessageLibre(derniereActivite, clock)).isTrue();
    }

    @Test
    @DisplayName("doit_refuser_un_message_libre_au_dela_de_24_heures")
    void doit_refuser_un_message_libre_au_dela_de_24_heures() {
        final Instant derniereActivite = clock.instant().minus(Duration.ofHours(25));

        assertThat(Regle24Heures.peutEnvoyerMessageLibre(derniereActivite, clock)).isFalse();
    }

    @Test
    @DisplayName("doit_refuser_si_aucune_activite")
    void doit_refuser_si_aucune_activite() {
        assertThat(Regle24Heures.peutEnvoyerMessageLibre(null, clock)).isFalse();
    }
}
