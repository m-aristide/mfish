package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParseurReponseIaTest {

    private final ParseurReponseIa parseur = new ParseurReponseIa();

    @Test
    @DisplayName("doit_parser_un_json_valide")
    void doit_parser_un_json_valide() {
        final String json = "{\"reponse\":\"ok\",\"intention\":\"commande\","
                + "\"commande\":{\"produits\":[{\"nom\":\"Attieke\",\"quantite\":2}],"
                + "\"quartier\":\"Cocody\",\"adresse\":null,\"dateSouhaitee\":\"2026-11-05\",\"complet\":true},"
                + "\"escalade\":false}";

        final ReponseIa reponse = parseur.parser(json);

        assertThat(reponse.reponse()).isEqualTo("ok");
        assertThat(reponse.intentionEnum()).isEqualTo(Intention.COMMANDE);
        assertThat(reponse.escalade()).isFalse();
        assertThat(reponse.commande().produits()).hasSize(1);
        assertThat(reponse.commande().dateSouhaitee()).isEqualTo("2026-11-05");
    }

    @Test
    @DisplayName("doit_ignorer_les_champs_inconnus")
    void doit_ignorer_les_champs_inconnus() {
        final String json = "{\"reponse\":\"ok\",\"intention\":\"question\",\"champInconnu\":123,\"escalade\":false}";

        assertThat(parseur.parser(json).reponse()).isEqualTo("ok");
    }

    @Test
    @DisplayName("doit_rejeter_un_json_invalide")
    void doit_rejeter_un_json_invalide() {
        assertThatThrownBy(() -> parseur.parser("pas-du-json"))
                .isInstanceOf(ReponseIaInvalideException.class);
    }

    @Test
    @DisplayName("doit_rejeter_un_json_tronque")
    void doit_rejeter_un_json_tronque() {
        assertThatThrownBy(() -> parseur.parser("{\"reponse\":\"ok\""))
                .isInstanceOf(ReponseIaInvalideException.class);
    }
}
