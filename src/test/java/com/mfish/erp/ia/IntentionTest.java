package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IntentionTest {

    @Test
    @DisplayName("doit_convertir_les_valeurs_minuscules")
    void doit_convertir_les_valeurs_minuscules() {
        assertThat(Intention.fromString("commande")).isEqualTo(Intention.COMMANDE);
        assertThat(Intention.fromString("question")).isEqualTo(Intention.QUESTION);
        assertThat(Intention.fromString("modification")).isEqualTo(Intention.MODIFICATION);
        assertThat(Intention.fromString("annulation")).isEqualTo(Intention.ANNULATION);
        assertThat(Intention.fromString("autre")).isEqualTo(Intention.AUTRE);
    }

    @Test
    @DisplayName("doit_retourner_autre_pour_valeur_inconnue_ou_nulle")
    void doit_retourner_autre_pour_valeur_inconnue_ou_nulle() {
        assertThat(Intention.fromString("inconnu")).isEqualTo(Intention.AUTRE);
        assertThat(Intention.fromString(null)).isEqualTo(Intention.AUTRE);
    }
}
