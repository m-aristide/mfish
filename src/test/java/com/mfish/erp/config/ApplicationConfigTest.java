package com.mfish.erp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationConfigTest {

    private final ApplicationConfig config = new ApplicationConfig();

    @Test
    @DisplayName("doit_creer_une_horloge_dans_le_fuseau_configure")
    void doit_creer_une_horloge_dans_le_fuseau_configure() {
        final ZoneId abidjan = ZoneId.of("Africa/Abidjan");

        final Clock clock = config.clock(new AppProperties(abidjan));

        assertThat(clock.getZone()).isEqualTo(abidjan);
    }

    @Test
    @DisplayName("doit_changer_de_jour_selon_le_fuseau_quand_minuit_utc_est_depasse")
    void doit_changer_de_jour_selon_le_fuseau_quand_minuit_utc_est_depasse() {
        final Instant instant = Instant.parse("2026-03-10T23:30:00Z");
        final Clock paris = Clock.fixed(instant, ZoneId.of("Europe/Paris"));
        final Clock abidjan = Clock.fixed(instant, ZoneId.of("Africa/Abidjan"));

        assertThat(LocalDate.now(paris)).isEqualTo(LocalDate.of(2026, 3, 11));
        assertThat(LocalDate.now(abidjan)).isEqualTo(LocalDate.of(2026, 3, 10));
    }
}
