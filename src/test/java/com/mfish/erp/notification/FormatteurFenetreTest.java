package com.mfish.erp.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormatteurFenetreTest {

    @Test
    @DisplayName("doit_formater_une_fenetre_de_30_minutes")
    void doit_formater_une_fenetre_de_30_minutes() {
        assertThat(FormatteurFenetre.formater(LocalTime.of(8, 30)))
                .isEqualTo("Votre livraison est prevue entre 08:00 et 09:00.");
    }

    @Test
    @DisplayName("doit_arrondir_a_5_minutes")
    void doit_arrondir_a_5_minutes() {
        assertThat(FormatteurFenetre.formater(LocalTime.of(8, 23)))
                .isEqualTo("Votre livraison est prevue entre 07:55 et 08:55.");
    }
}
