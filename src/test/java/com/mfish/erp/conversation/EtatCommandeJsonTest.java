package com.mfish.erp.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EtatCommandeJsonTest {

    private static EtatCommande etatComplet() {
        return new EtatCommande(List.of(new LigneEtatCommande("Attieke", 3)), "Cocody",
                "Rue 12", LocalDate.of(2026, 11, 19), "12h", StatutEtatCommande.CONFIRMEE);
    }

    @Test
    @DisplayName("doit_faire_un_aller_retour_complet_en_json")
    void doit_faire_un_aller_retour_complet_en_json() {
        final EtatCommande relu =
                EtatCommandeJson.depuisJson(EtatCommandeJson.versJson(etatComplet()));

        assertThat(relu).isEqualTo(etatComplet());
        assertThat(relu.champsManquants()).isEmpty();
    }

    @Test
    @DisplayName("doit_exposer_les_champs_manquants_dans_le_json")
    void doit_exposer_les_champs_manquants_dans_le_json() {
        final String json = EtatCommandeJson.versJson(EtatCommande.vide());

        assertThat(json).contains("\"champsManquants\"")
                .contains("la quantite")
                .contains("\"statut\":\"EN_COURS\"");
    }

    @Test
    @DisplayName("doit_degrader_vers_un_etat_vide_si_le_json_est_absent_ou_illisible")
    void doit_degrader_vers_un_etat_vide_si_le_json_est_absent_ou_illisible() {
        assertThat(EtatCommandeJson.depuisJson(null)).isEqualTo(EtatCommande.vide());
        assertThat(EtatCommandeJson.depuisJson("  ")).isEqualTo(EtatCommande.vide());
        assertThat(EtatCommandeJson.depuisJson("{pas du json")).isEqualTo(EtatCommande.vide());
        assertThat(EtatCommandeJson.depuisJson("{}")).isEqualTo(EtatCommande.vide());
    }

    @Test
    @DisplayName("doit_ignorer_les_champs_inconnus_et_tolerer_un_statut_inconnu")
    void doit_ignorer_les_champs_inconnus_et_tolerer_un_statut_inconnu() {
        final String json = "{\"lignes\":[{\"nom\":\"Attieke\",\"quantite\":2}],"
                + "\"quartier\":\"Cocody\",\"champInconnu\":\"x\",\"statut\":\"ARCHIVEE\"}";

        final EtatCommande etat = EtatCommandeJson.depuisJson(json);

        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Attieke", 2));
        assertThat(etat.quartier()).isEqualTo("Cocody");
        assertThat(etat.statut()).isEqualTo(StatutEtatCommande.EN_COURS);
    }

    @Test
    @DisplayName("doit_accepter_une_date_au_format_francais")
    void doit_accepter_une_date_au_format_francais() {
        assertThat(EtatCommandeJson.analyserDate("19/11/2026"))
                .isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(EtatCommandeJson.analyserDate("2026-11-19"))
                .isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(EtatCommandeJson.analyserDate("demain")).isNull();
        assertThat(EtatCommandeJson.analyserDate(null)).isNull();
    }

    @Test
    @DisplayName("doit_tolerer_un_etat_null")
    void doit_tolerer_un_etat_null() {
        assertThat(EtatCommandeJson.versJson(null)).isEqualTo(EtatCommandeJson.versJson(
                EtatCommande.vide()));
    }
}
