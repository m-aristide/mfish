package com.mfish.erp.tournee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OptimisationLocaleTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID U2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID U3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    @DisplayName("doit_retourner_vide_sans_arret")
    void doit_retourner_vide_sans_arret() {
        final var input = new TourneeRoutingInput(new Coordonnees(0, 0), LocalTime.of(8, 0), List.of());

        assertThat(OptimisationLocale.optimiser(input, 30, 5)).isEmpty();
    }

    @Test
    @DisplayName("doit_respecter_l_ordre_par_defaut_des_quartiers")
    void doit_respecter_l_ordre_par_defaut_des_quartiers() {
        final var input = new TourneeRoutingInput(new Coordonnees(5.32, -4.01), LocalTime.of(8, 0),
                List.of(
                        new ArretAOptimiser(U1, new Coordonnees(5.35, -3.98), 2),
                        new ArretAOptimiser(U2, new Coordonnees(5.34, -3.99), 0),
                        new ArretAOptimiser(U3, new Coordonnees(5.33, -3.97), 1)));

        final List<ArretPlanifie> resultat = OptimisationLocale.optimiser(input, 30, 0);

        assertThat(resultat).extracting(ArretPlanifie::commandeId).containsExactly(U2, U3, U1);
    }

    @Test
    @DisplayName("doit_utiliser_le_plus_proche_voisin_dans_le_meme_quartier")
    void doit_utiliser_le_plus_proche_voisin_dans_le_meme_quartier() {
        final Coordonnees depart = new Coordonnees(5.32, -4.01);
        final var input = new TourneeRoutingInput(depart, LocalTime.of(8, 0),
                List.of(
                        new ArretAOptimiser(U1, new Coordonnees(5.40, -3.90), 0),
                        new ArretAOptimiser(U2, new Coordonnees(5.33, -4.00), 0)));

        final List<ArretPlanifie> resultat = OptimisationLocale.optimiser(input, 30, 0);

        assertThat(resultat).extracting(ArretPlanifie::commandeId).containsExactly(U2, U1);
    }

    @Test
    @DisplayName("doit_calculer_les_heures_avec_duree_d_arret")
    void doit_calculer_les_heures_avec_duree_d_arret() {
        final var input = new TourneeRoutingInput(new Coordonnees(0, 0), LocalTime.of(8, 0),
                List.of(
                        new ArretAOptimiser(U1, new Coordonnees(0, 0.1), 0),
                        new ArretAOptimiser(U2, new Coordonnees(0, 0.2), 1)));

        final List<ArretPlanifie> resultat = OptimisationLocale.optimiser(input, 60, 5);

        assertThat(resultat).extracting(ArretPlanifie::heureArrivee)
                .containsExactly(LocalTime.of(8, 11), LocalTime.of(8, 27));
    }

    @Test
    @DisplayName("doit_rejeter_une_vitesse_non_positive")
    void doit_rejeter_une_vitesse_non_positive() {
        final var input = new TourneeRoutingInput(new Coordonnees(0, 0), LocalTime.of(8, 0),
                List.of(new ArretAOptimiser(U1, new Coordonnees(0, 0.1), 0)));

        assertThatThrownBy(() -> OptimisationLocale.optimiser(input, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
