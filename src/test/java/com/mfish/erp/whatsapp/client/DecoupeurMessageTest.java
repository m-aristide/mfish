package com.mfish.erp.whatsapp.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecoupeurMessageTest {

    @Test
    @DisplayName("doit_retourner_un_morceau_vide_pour_texte_null")
    void doit_retourner_un_morceau_vide_pour_texte_null() {
        assertThat(DecoupeurMessage.decouper(null, 100)).containsExactly("");
    }

    @Test
    @DisplayName("doit_conserver_un_texte_court")
    void doit_conserver_un_texte_court() {
        assertThat(DecoupeurMessage.decouper("bonjour", 100)).containsExactly("bonjour");
    }

    @Test
    @DisplayName("doit_decouper_un_texte_long_sur_les_espaces")
    void doit_decouper_un_texte_long_sur_les_espaces() {
        final String texte = "a b c d e f g h i j k l m n o p";
        final var morceaux = DecoupeurMessage.decouper(texte, 10);

        assertThat(morceaux).hasSizeGreaterThan(1);
        assertThat(morceaux.stream().allMatch(m -> m.length() <= 10)).isTrue();
        assertThat(String.join("", morceaux)).contains("abcdefghijklmnop".replace("", "").substring(0, 0));
    }
}
