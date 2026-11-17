package com.mfish.erp.whatsapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NormaliseurTelephoneTest {

    @Test
    @DisplayName("doit_normaliser_un_numero_international")
    void doit_normaliser_un_numero_international() {
        assertThat(NormaliseurTelephone.normaliser("+225 07 00 00 00 01")).isEqualTo("+2250700000001");
    }

    @Test
    @DisplayName("doit_ajouter_le_plus_si_absent")
    void doit_ajouter_le_plus_si_absent() {
        assertThat(NormaliseurTelephone.normaliser("2250700000001")).isEqualTo("+2250700000001");
    }

    @Test
    @DisplayName("doit_remplacer_00_par_le_plus")
    void doit_remplacer_00_par_le_plus() {
        assertThat(NormaliseurTelephone.normaliser("002250700000001")).isEqualTo("+2250700000001");
    }

    @Test
    @DisplayName("doit_rejeter_un_numero_local_sans_indicatif")
    void doit_rejeter_un_numero_local_sans_indicatif() {
        assertThat(NormaliseurTelephone.normaliser("0700000001")).isNull();
    }

    @Test
    @DisplayName("doit_rejeter_une_entree_null")
    void doit_rejeter_une_entree_null() {
        assertThat(NormaliseurTelephone.normaliser(null)).isNull();
    }
}
