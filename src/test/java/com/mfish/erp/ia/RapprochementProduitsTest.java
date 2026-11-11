package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mfish.erp.produit.Produit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RapprochementProduitsTest {

    private RapprochementProduits rapprochement;
    private List<Produit> catalogue;

    @BeforeEach
    void setUp() {
        rapprochement = new RapprochementProduits();
        catalogue = List.of(
                new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg"),
                new Produit("Poulet braise", 5000, "piece", true, "/images/default-produit.svg"),
                new Produit("Jus de bissap", 1000, "bouteille", true, "/images/default-produit.svg"),
                new Produit("Riz", 500, "plat", true, "/images/default-produit.svg"),
                new Produit("Ris", 600, "plat", true, "/images/default-produit.svg"));
    }

    @Test
    @DisplayName("doit_trouver_un_produit_exact")
    void doit_trouver_un_produit_exact() {
        final var resultat = rapprochement.rapprocher(
                List.of(new ProduitIa("Attieke", 2)), catalogue);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).produit().getNom()).isEqualTo("Attieke");
        assertThat(resultat.get(0).quantite()).isEqualTo(2);
    }

    @Test
    @DisplayName("doit_trouver_une_faute_de_frappe")
    void doit_trouver_une_faute_de_frappe() {
        final var resultat = rapprochement.rapprocher(
                List.of(new ProduitIa("Atieke", 1)), catalogue);

        assertThat(resultat.get(0).produit().getNom()).isEqualTo("Attieke");
    }

    @Test
    @DisplayName("doit_trouver_un_pluriel")
    void doit_trouver_un_pluriel() {
        final var resultat = rapprochement.rapprocher(
                List.of(new ProduitIa("Attiékés", 1)), catalogue);

        assertThat(resultat.get(0).produit().getNom()).isEqualTo("Attieke");
    }

    @Test
    @DisplayName("doit_rejeter_un_produit_inconnu")
    void doit_rejeter_un_produit_inconnu() {
        assertThatThrownBy(() -> rapprochement.rapprocher(
                List.of(new ProduitIa("Pizza", 1)), catalogue))
                .isInstanceOf(ProduitInconnuException.class);
    }

    @Test
    @DisplayName("doit_rejeter_un_produit_ambigu")
    void doit_rejeter_un_produit_ambigu() {
        assertThatThrownBy(() -> rapprochement.rapprocher(
                List.of(new ProduitIa("Rix", 1)), catalogue))
                .isInstanceOf(ProduitAmbiguException.class);
    }
}
