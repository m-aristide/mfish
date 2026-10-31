package com.mfish.erp.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validation declarative des records de configuration (contraintes Jakarta Bean Validation).
 */
class ConfigurationPropertiesValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static RetryProperties retry() {
        return new RetryProperties(3, Duration.ofMillis(200), 2.0, Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("doit_valider_jwt_quand_secret_et_duree_presents")
    void doit_valider_jwt_quand_secret_et_duree_presents() {
        final var violations = validator.validate(
                new JwtProperties("secret-de-32-caracteres-minimum-000", Duration.ofHours(12)));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("doit_rejeter_jwt_quand_secret_vide")
    void doit_rejeter_jwt_quand_secret_vide() {
        final var violations = validator.validate(new JwtProperties("", Duration.ofHours(12)));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("secret");
    }

    @Test
    @DisplayName("doit_rejeter_admin_quand_mot_de_passe_vide")
    void doit_rejeter_admin_quand_mot_de_passe_vide() {
        final var violations = validator.validate(new AdminProperties("admin", ""));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("passwordHash");
    }

    @Test
    @DisplayName("doit_rejeter_cors_quand_aucune_origine")
    void doit_rejeter_cors_quand_aucune_origine() {
        final var violations = validator.validate(new CorsProperties(List.of()));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("allowedOrigins");
    }

    @Test
    @DisplayName("doit_rejeter_retry_quand_multiplicateur_non_positif")
    void doit_rejeter_retry_quand_multiplicateur_non_positif() {
        final var violations = validator.validate(
                new RetryProperties(3, Duration.ofMillis(200), 0.0, Duration.ofSeconds(2)));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("multiplier");
    }

    @Test
    @DisplayName("doit_rejeter_routexl_quand_max_stops_non_positif")
    void doit_rejeter_routexl_quand_max_stops_non_positif() {
        final var violations = validator.validate(new RoutexlProperties(
                "https://api.routexl.com", "u", "p", 0,
                Duration.ofSeconds(3), Duration.ofSeconds(30), retry()));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("maxStops");
    }

    @Test
    @DisplayName("doit_rejeter_chatwoot_quand_politique_retry_absente")
    void doit_rejeter_chatwoot_quand_politique_retry_absente() {
        final var violations = validator.validate(new ChatwootProperties(
                "http://localhost:3000", "token", "1", "secret",
                Duration.ofSeconds(3), Duration.ofSeconds(10), null));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("retry");
    }

    @Test
    @DisplayName("doit_accepter_les_proprietes_externes_completes")
    void doit_accepter_les_proprietes_externes_completes() {
        assertThat(validator.validate(new DeepSeekProperties(
                "https://api.deepseek.com", "key", "deepseek-chat",
                Duration.ofSeconds(3), Duration.ofSeconds(30), retry()))).isEmpty();

        assertThat(validator.validate(new GeocodingProperties(
                "nominatim", "https://nominatim.openstreetmap.org",
                Duration.ofSeconds(3), Duration.ofSeconds(10), retry()))).isEmpty();
    }
}
