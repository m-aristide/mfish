package com.mfish.erp.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mfish.erp.config.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "secret-de-test-32-caracteres-minimum-000";

    private JwtService service(final Clock clock) {
        return new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)), clock);
    }

    @Test
    @DisplayName("doit_generer_et_valider_un_jeton")
    void doit_generer_et_valider_un_jeton() {
        final JwtService service = service(Clock.systemUTC());

        assertThat(service.valider(service.generer("admin"))).contains("admin");
    }

    @Test
    @DisplayName("doit_refuser_un_jeton_malforme")
    void doit_refuser_un_jeton_malforme() {
        assertThat(service(Clock.systemUTC()).valider("pas-un-jwt")).isEmpty();
    }

    @Test
    @DisplayName("doit_refuser_un_jeton_signe_avec_un_autre_secret")
    void doit_refuser_un_jeton_signe_avec_un_autre_secret() {
        final JwtService autre = new JwtService(
                new JwtProperties("autre-secret-de-32-caracteres-000000", Duration.ofHours(1)),
                Clock.systemUTC());
        final String token = autre.generer("admin");

        assertThat(service(Clock.systemUTC()).valider(token)).isEmpty();
    }

    @Test
    @DisplayName("doit_refuser_un_jeton_expire")
    void doit_refuser_un_jeton_expire() {
        final Clock passe = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
        final String token = service(passe).generer("admin");
        final Clock futur = Clock.fixed(passe.instant().plus(Duration.ofHours(2)), ZoneId.of("UTC"));

        assertThat(service(futur).valider(token)).isEmpty();
    }

    @Test
    @DisplayName("doit_rejeter_un_secret_trop_court")
    void doit_rejeter_un_secret_trop_court() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("court", Duration.ofHours(1)),
                Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
