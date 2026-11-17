package com.mfish.erp.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("doit_accepter_les_proprietes_quand_le_fuseau_est_renseigne")
    void doit_accepter_les_proprietes_quand_le_fuseau_est_renseigne() {
        final var violations = validator.validate(new AppProperties(ZoneId.of("Africa/Abidjan")));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("doit_rejeter_les_proprietes_quand_le_fuseau_est_absent")
    void doit_rejeter_les_proprietes_quand_le_fuseau_est_absent() {
        final var violations = validator.validate(new AppProperties(null));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("timezone");
    }
}
