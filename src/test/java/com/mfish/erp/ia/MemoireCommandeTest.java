package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.conversation.EtatCommande;
import com.mfish.erp.conversation.LigneEtatCommande;
import com.mfish.erp.conversation.StatutEtatCommande;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemoireCommandeTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"),
            ZoneId.of("Africa/Douala"));

    private final MemoireCommande memoire = new MemoireCommande(clock);

    private static ReponseIa reponse(final String intention, final List<ProduitIa> produits,
                                     final String quartier, final String date) {
        return new ReponseIa("ok", intention,
                new CommandeIa(produits, quartier, null, date, null, false), false);
    }

    private static EtatCommande attieke(final int quantite) {
        return new EtatCommande(List.of(new LigneEtatCommande("Attieke", quantite)), null, null,
                null, null, StatutEtatCommande.EN_COURS);
    }

    @Test
    @DisplayName("doit_completer_l_etat_avec_un_message_partiel")
    void doit_completer_l_etat_avec_un_message_partiel() {
        final EtatCommande etat = memoire.fusionner(attieke(3),
                reponse("commande", List.of(), "Cocody", "2026-11-19"), "Cocody le 19/11");

        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Attieke", 3));
        assertThat(etat.quartier()).isEqualTo("Cocody");
        assertThat(etat.dateLivraison()).isEqualTo(DATE);
        assertThat(etat.complet()).isTrue();
    }

    @Test
    @DisplayName("ne_doit_jamais_effacer_une_valeur_connue_avec_du_vide")
    void ne_doit_jamais_effacer_une_valeur_connue_avec_du_vide() {
        final EtatCommande connu = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", "Rue 12", DATE, "12h",
                StatutEtatCommande.EN_COURS);

        final EtatCommande etat = memoire.fusionner(connu,
                reponse("question", null, "  ", null), "et sinon ?");

        assertThat(etat).isEqualTo(connu);
    }

    @Test
    @DisplayName("doit_remplacer_la_quantite_quand_le_client_corrige")
    void doit_remplacer_la_quantite_quand_le_client_corrige() {
        final EtatCommande etat = memoire.fusionner(attieke(3),
                reponse("modification", List.of(new ProduitIa("Attieke", 5)), null, null),
                "non, mettez 5 kilos");

        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Attieke", 5));
    }

    @Test
    @DisplayName("doit_additionner_la_quantite_quand_le_client_en_ajoute")
    void doit_additionner_la_quantite_quand_le_client_en_ajoute() {
        final EtatCommande etat = memoire.fusionner(attieke(3),
                reponse("modification", List.of(new ProduitIa("Attieke", 2)), null, null),
                "ajoute 2 kilos");

        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Attieke", 5));
    }

    @Test
    @DisplayName("doit_additionner_sur_un_unique_produit_sans_le_renommer")
    void doit_additionner_sur_un_unique_produit_sans_le_renommer() {
        final EtatCommande etat = memoire.fusionner(attieke(3),
                reponse("modification", List.of(new ProduitIa("", 1)), null, null),
                "encore 1 kilo");

        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Attieke", 4));
    }

    @Test
    @DisplayName("doit_ajouter_une_ligne_pour_un_second_produit")
    void doit_ajouter_une_ligne_pour_un_second_produit() {
        final EtatCommande etat = memoire.fusionner(attieke(3),
                reponse("commande", List.of(new ProduitIa("Riz", 2)), null, null),
                "et 2 kilos de riz");

        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Attieke", 3),
                new LigneEtatCommande("Riz", 2));
    }

    @Test
    @DisplayName("doit_rapprocher_un_nom_de_produit_avec_une_faute_de_frappe")
    void doit_rapprocher_un_nom_de_produit_avec_une_faute_de_frappe() {
        final EtatCommande etat = memoire.fusionner(attieke(3),
                reponse("modification", List.of(new ProduitIa("Attiek", 4)), null, null),
                "plutot 4");

        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Attieke", 4));
    }

    @Test
    @DisplayName("doit_ignorer_une_extraction_sans_produit_ni_quantite")
    void doit_ignorer_une_extraction_sans_produit_ni_quantite() {
        final EtatCommande etat = memoire.fusionner(EtatCommande.vide(),
                reponse("commande", List.of(new ProduitIa("Attieke", 0)), null, null), "attieke");

        assertThat(etat.lignes()).isEmpty();
        assertThat(etat.champsManquants()).contains(EtatCommande.CHAMP_QUANTITE);
    }

    @Test
    @DisplayName("ne_doit_pas_inventer_une_quantite_sans_indication_du_client")
    void ne_doit_pas_inventer_une_quantite_sans_indication_du_client() {
        final EtatCommande etat = memoire.fusionner(EtatCommande.vide(),
                reponse("commande", List.of(new ProduitIa("Attieke", 1)), null, null),
                "je veux de l'attieke");

        assertThat(etat.lignes()).isEmpty();
        assertThat(etat.champsManquants()).contains(EtatCommande.CHAMP_QUANTITE);
    }

    @Test
    @DisplayName("doit_reconnaitre_les_remerciements_et_salutations")
    void doit_reconnaitre_les_remerciements_et_salutations() {
        assertThat(memoire.estRemerciementOuSalutation("Merci")).isTrue();
        assertThat(memoire.estRemerciementOuSalutation("Bonjour")).isTrue();
        assertThat(memoire.estRemerciementOuSalutation("🙏")).isTrue();
        assertThat(memoire.estRemerciementOuSalutation("Oui je confirme")).isFalse();
        assertThat(memoire.estRemerciementOuSalutation(null)).isFalse();
    }

    @Test
    @DisplayName("doit_remettre_la_memoire_a_zero_sur_annulation")
    void doit_remettre_la_memoire_a_zero_sur_annulation() {
        final EtatCommande etat = memoire.fusionner(attieke(3),
                new ReponseIa("Commande annulee.", "annulation", null, false), "annulez tout");

        assertThat(etat).isEqualTo(EtatCommande.vide());
    }

    @Test
    @DisplayName("doit_demarrer_une_nouvelle_commande_sur_une_autre_date_apres_confirmation")
    void doit_demarrer_une_nouvelle_commande_sur_une_autre_date_apres_confirmation() {
        final EtatCommande confirmee = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, DATE, null,
                StatutEtatCommande.ENREGISTREE);

        final EtatCommande etat = memoire.fusionner(confirmee,
                reponse("commande", List.of(new ProduitIa("Riz", 2)), null, "2026-11-25"),
                "je veux 2 kilos de riz pour le 25/11");

        assertThat(etat.statut()).isEqualTo(StatutEtatCommande.EN_COURS);
        assertThat(etat.lignes()).containsExactly(new LigneEtatCommande("Riz", 2));
        assertThat(etat.quartier()).isNull();
        assertThat(etat.dateLivraison()).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    @DisplayName("doit_ne_questionner_que_sur_les_informations_manquantes")
    void doit_ne_questionner_que_sur_les_informations_manquantes() {
        final String question = memoire.questionSuivante(attieke(3));

        assertThat(question)
                .contains("J'ai bien note : 3 kg de Attieke")
                .contains("Il me manque le quartier, la date de livraison")
                .doesNotContain("le produit");
    }

    @Test
    @DisplayName("doit_reconnaitre_les_confirmations_courtes")
    void doit_reconnaitre_les_confirmations_courtes() {
        assertThat(memoire.estConfirmation("Oui")).isTrue();
        assertThat(memoire.estConfirmation("d'accord")).isTrue();
        assertThat(memoire.estConfirmation("C'est bon, confirme")).isTrue();
        assertThat(memoire.estConfirmation("Non merci")).isFalse();
        assertThat(memoire.estConfirmation(null)).isFalse();
        assertThat(memoire.estConfirmation("je voudrais savoir si vous livrez le dimanche "
                + "a Obobogo")).isFalse();
    }

    @Test
    @DisplayName("doit_traduire_l_etat_en_commande_exploitable")
    void doit_traduire_l_etat_en_commande_exploitable() {
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", "Rue 12", DATE, "12h",
                StatutEtatCommande.EN_COURS);

        final CommandeIa commande = memoire.versCommandeIa(etat);

        assertThat(commande.produits()).containsExactly(new ProduitIa("Attieke", 3));
        assertThat(commande.quartier()).isEqualTo("Cocody");
        assertThat(commande.adresse()).isEqualTo("Rue 12");
        assertThat(commande.dateSouhaitee()).isEqualTo("2026-11-19");
        assertThat(commande.heureSouhaitee()).isEqualTo("12h");
        assertThat(commande.complet()).isTrue();
    }

    @Test
    @DisplayName("doit_formuler_les_recapitulatifs")
    void doit_formuler_les_recapitulatifs() {
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, DATE, null,
                StatutEtatCommande.ENREGISTREE);

        assertThat(memoire.recapitulatif(etat)).startsWith("Votre commande est enregistree");
        assertThat(memoire.recapitulatif(etat)).contains("3 kg de Attieke");
        assertThat(memoire.recapitulatifAConfirmer(etat))
                .startsWith("Recapitulatif de votre commande")
                .endsWith("Confirmez-vous cette commande ?");
    }

    @Test
    @DisplayName("doit_resoudre_les_dates_relatives_a_partir_du_fuseau_metier")
    void doit_resoudre_les_dates_relatives_a_partir_du_fuseau_metier() {
        final LocalDate aujourdhui = LocalDate.of(2026, 11, 23);

        assertThat(memoire.fusionner(EtatCommande.vide(),
                reponse("commande", List.of(), null, null), "Je veux etre livre demain")
                .dateLivraison()).isEqualTo(aujourdhui.plusDays(1));
        assertThat(memoire.fusionner(EtatCommande.vide(),
                reponse("commande", List.of(), null, null), "apres-demain")
                .dateLivraison()).isEqualTo(aujourdhui.plusDays(2));
        assertThat(memoire.fusionner(EtatCommande.vide(),
                reponse("commande", List.of(), null, null), "livraison aujourd'hui")
                .dateLivraison()).isEqualTo(aujourdhui);
    }
}
