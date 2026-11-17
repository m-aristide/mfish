package com.mfish.erp.tournee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecoupageLotsTest {

    @Test
    @DisplayName("doit_decouper_en_lots_de_taille_maximale")
    void doit_decouper_en_lots_de_taille_maximale() {
        assertThat(DecoupageLots.decouper(List.of(1, 2, 3, 4, 5), 2))
                .containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
    }

    @Test
    @DisplayName("doit_retourner_un_seul_lot_si_la_taille_est_suffisante")
    void doit_retourner_un_seul_lot_si_la_taille_est_suffisante() {
        assertThat(DecoupageLots.decouper(List.of(1, 2), 10))
                .containsExactly(List.of(1, 2));
    }

    @Test
    @DisplayName("doit_retourner_aucun_lot_pour_une_liste_vide")
    void doit_retourner_aucun_lot_pour_une_liste_vide() {
        assertThat(DecoupageLots.decouper(List.of(), 2)).isEmpty();
    }

    @Test
    @DisplayName("doit_rejeter_une_taille_non_positive")
    void doit_rejeter_une_taille_non_positive() {
        assertThatThrownBy(() -> DecoupageLots.decouper(List.of(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
