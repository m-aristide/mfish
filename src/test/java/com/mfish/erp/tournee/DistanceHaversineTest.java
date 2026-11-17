package com.mfish.erp.tournee;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DistanceHaversineTest {

    @Test
    @DisplayName("doit_retourner_zero_pour_le_meme_point")
    void doit_retourner_zero_pour_le_meme_point() {
        assertThat(DistanceHaversine.kilometres(new Coordonnees(5.35, -3.98),
                new Coordonnees(5.35, -3.98))).isEqualTo(0.0);
    }

    @Test
    @DisplayName("doit_correspondre_a_un_degre_de_longitude_a_l_equateur")
    void doit_correspondre_a_un_degre_de_longitude_a_l_equateur() {
        final double distance = DistanceHaversine.kilometres(
                new Coordonnees(0, 0), new Coordonnees(0, 1));

        assertThat(distance).isBetween(110.0, 112.0);
    }

    @Test
    @DisplayName("doit_etre_symetrique")
    void doit_etre_symetrique() {
        final Coordonnees a = new Coordonnees(5.35, -3.98);
        final Coordonnees b = new Coordonnees(5.32, -4.01);

        assertThat(DistanceHaversine.kilometres(a, b))
                .isEqualTo(DistanceHaversine.kilometres(b, a));
    }
}
