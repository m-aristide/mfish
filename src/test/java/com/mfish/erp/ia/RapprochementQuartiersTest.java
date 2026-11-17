package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.quartier.Quartier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RapprochementQuartiersTest {

    private final RapprochementQuartiers rapprochement = new RapprochementQuartiers();

    private static Quartier quartier(final String nom) {
        return new Quartier(nom, "Centre", 3.85, 11.50, 0, true);
    }

    @Test
    @DisplayName("retourne le quartier exact unique sans ambiguite")
    void retourne_le_quartier_exact() {
        final var resultat = rapprochement.rapprocher("Mvan", List.of(quartier("Mvan")));

        assertThat(resultat).isPresent();
        assertThat(resultat.get().quartier().getNom()).isEqualTo("Mvan");
        assertThat(resultat.get().ambigu()).isFalse();
    }

    @Test
    @DisplayName("retourne le premier et signale l'ambiguite si plusieurs correspondent")
    void retourne_le_premier_si_ambigu() {
        final var resultat = rapprochement.rapprocher("Mvan",
                List.of(quartier("Mvan"), quartier("Mvan")));

        assertThat(resultat).isPresent();
        assertThat(resultat.get().ambigu()).isTrue();
        assertThat(resultat.get().quartier().getNom()).isEqualTo("Mvan");
    }

    @Test
    @DisplayName("tolere une faute de frappe")
    void tolere_une_faute_de_frappe() {
        final var resultat = rapprochement.rapprocher("Mvann", List.of(quartier("Mvan")));

        assertThat(resultat).isPresent();
        assertThat(resultat.get().ambigu()).isFalse();
        assertThat(resultat.get().quartier().getNom()).isEqualTo("Mvan");
    }

    @Test
    @DisplayName("retourne vide si aucun quartier ne correspond")
    void retourne_vide_si_inconnu() {
        assertThat(rapprochement.rapprocher("Inconnu", List.of(quartier("Mvan")))).isEmpty();
    }

    @Test
    @DisplayName("retourne vide si le nom est vide")
    void retourne_vide_si_nom_vide() {
        assertThat(rapprochement.rapprocher("   ", List.of(quartier("Mvan")))).isEmpty();
    }
}