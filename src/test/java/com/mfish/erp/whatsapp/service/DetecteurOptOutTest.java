package com.mfish.erp.whatsapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DetecteurOptOutTest {

    @Test
    @DisplayName("doit_detecter_le_mot_stop")
    void doit_detecter_le_mot_stop() {
        assertThat(DetecteurOptOut.estOpposition("STOP")).isTrue();
    }

    @Test
    @DisplayName("doit_detecter_arret_avec_accent")
    void doit_detecter_arret_avec_accent() {
        assertThat(DetecteurOptOut.estOpposition("ARRÊT s il vous plait")).isTrue();
    }

    @Test
    @DisplayName("doit_detecter_la_desinscription")
    void doit_detecter_la_desinscription() {
        assertThat(DetecteurOptOut.estOpposition("je veux me désinscrire")).isTrue();
    }

    @Test
    @DisplayName("doit_ignorer_un_message_normal")
    void doit_ignorer_un_message_normal() {
        assertThat(DetecteurOptOut.estOpposition("je voudrais 2 attieke")).isFalse();
    }

    @Test
    @DisplayName("doit_ignorer_un_texte_null")
    void doit_ignorer_un_texte_null() {
        assertThat(DetecteurOptOut.estOpposition(null)).isFalse();
    }
}
