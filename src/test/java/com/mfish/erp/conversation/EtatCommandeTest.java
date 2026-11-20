package com.mfish.erp.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EtatCommandeTest {

    @Test
    @DisplayName("doit_lister_les_champs_manquants_dans_l_ordre_de_collecte")
    void doit_lister_les_champs_manquants_dans_l_ordre_de_collecte() {
        assertThat(EtatCommande.vide().champsManquants()).containsExactly(
                EtatCommande.CHAMP_PRODUIT, EtatCommande.CHAMP_QUARTIER, EtatCommande.CHAMP_DATE);
        assertThat(EtatCommande.vide().complet()).isFalse();
    }

    @Test
    @DisplayName("doit_demander_la_quantite_quand_la_ligne_n_en_a_pas")
    void doit_demander_la_quantite_quand_la_ligne_n_en_a_pas() {
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 0)), "Cocody", "Rue 12",
                LocalDate.of(2026, 11, 19), null, StatutEtatCommande.EN_COURS);

        assertThat(etat.champsManquants()).containsExactly(EtatCommande.CHAMP_QUANTITE);
        assertThat(etat.complet()).isFalse();
    }

    @Test
    @DisplayName("doit_etre_complete_avec_produit_quantite_quartier_et_date")
    void doit_etre_complete_avec_produit_quantite_quartier_et_date() {
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null,
                LocalDate.of(2026, 11, 19), "12h", StatutEtatCommande.EN_COURS);

        assertThat(etat.complet()).isTrue();
        assertThat(etat.champsManquants()).isEmpty();
    }

    @Test
    @DisplayName("doit_resumer_les_informations_deja_collectees")
    void doit_resumer_les_informations_deja_collectees() {
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", "Rue 12",
                LocalDate.of(2026, 11, 19), "12h", StatutEtatCommande.CONFIRMEE);

        assertThat(etat.resume())
                .contains("3 kg de Attieke")
                .contains("quartier Cocody")
                .contains("adresse Rue 12")
                .contains("livraison le 19/11/2026")
                .contains("vers 12h");
        assertThat(etat.estConfirmee()).isTrue();
    }

    @Test
    @DisplayName("doit_ignorer_une_ligne_sans_quantite_dans_le_resume")
    void doit_ignorer_une_ligne_sans_quantite_dans_le_resume() {
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 0)), null, null, null, null, null);

        assertThat(etat.resume()).isEmpty();
        assertThat(etat.statut()).isEqualTo(StatutEtatCommande.EN_COURS);
    }

    @Test
    @DisplayName("doit_proteger_la_liste_de_lignes_contre_les_modifications_externes")
    void doit_proteger_la_liste_de_lignes_contre_les_modifications_externes() {
        final List<LigneEtatCommande> lignes =
                Arrays.asList(new LigneEtatCommande("Attieke", 1));
        final EtatCommande etat =
                new EtatCommande(lignes, null, null, null, null, StatutEtatCommande.EN_COURS);

        lignes.set(0, new LigneEtatCommande("Riz", 9));

        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Attieke", 1));
    }
}
