package com.mfish.erp.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MasquageTest {

    @Test
    @DisplayName("doit_masquer_un_numero_long")
    void doit_masquer_un_numero_long() {
        final String masque = Masquage.telephone("+2250700000001");

        assertThat(masque).doesNotContain("0700000001");
        assertThat(masque).startsWith("+225");
    }

    @Test
    @DisplayName("doit_masquer_un_numero_court")
    void doit_masquer_un_numero_court() {
        assertThat(Masquage.telephone("123")).isEqualTo("***");
    }

    @Test
    @DisplayName("doit_gerer_une_entree_null")
    void doit_gerer_une_entree_null() {
        assertThat(Masquage.telephone(null)).isEqualTo("?");
    }
}
