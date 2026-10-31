package com.mfish.erp.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.tournee.StatutArret;
import com.mfish.erp.utilisateur.Role;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void initialiser() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("client : nom vide et telephone invalide rejetes")
    void client_invalide_rejete() {
        final var formulaire = new ClientFormulaire("", "abc", null, null, null, null, null, null);
        assertThat(validator.validate(formulaire)).isNotEmpty();
    }

    @Test
    @DisplayName("commande : liste de lignes vide rejetee")
    void commande_sans_ligne_rejetee() {
        final var formulaire = new CommandeFormulaire(UUID.randomUUID(), List.of(),
                ModePaiement.ESPECES, null, null);
        assertThat(validator.validate(formulaire))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("lignes"));
    }

    @Test
    @DisplayName("ligne : quantite negative rejetee")
    void ligne_quantite_negative_rejetee() {
        final var ligne = new LigneCommandeFormulaire(UUID.randomUUID(), -1);
        assertThat(validator.validate(ligne))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("quantite"));
    }

    @Test
    @DisplayName("paiement : montant non positif rejete")
    void paiement_montant_non_positif_rejete() {
        assertThat(validator.validate(new PaiementFormulaire(0))).isNotEmpty();
    }

    @Test
    @DisplayName("statut : valeur nulle rejetee")
    void statut_null_rejete() {
        assertThat(validator.validate(new StatutFormulaire(null))).isNotEmpty();
    }

    @Test
    @DisplayName("arret : statut null rejete")
    void arret_statut_null_rejete() {
        assertThat(validator.validate(new ArretFormulaire(null, 0))).isNotEmpty();
    }

    @Test
    @DisplayName("utilisateur : mot de passe vide rejete")
    void utilisateur_mot_de_passe_vide_rejete() {
        assertThat(validator.validate(new UtilisateurFormulaire("user", "", Role.ADMIN))).isNotEmpty();
    }

    @Test
    @DisplayName("connaissance : contenu vide rejete")
    void connaissance_contenu_vide_rejete() {
        assertThat(validator.validate(new ConnaissanceFormulaire(""))).isNotEmpty();
    }

    @Test
    @DisplayName("formulaires valides acceptes")
    void formulaires_valides_acceptes() {
        assertThat(validator.validate(new ClientFormulaire("Awa", "+2250700000001", null,
                null, null, null, null, null))).isEmpty();
        assertThat(validator.validate(new ProduitFormulaire("Attieke", 1000, "plat", true))).isEmpty();
        assertThat(validator.validate(new LigneCommandeFormulaire(UUID.randomUUID(), 2))).isEmpty();
        assertThat(validator.validate(new StatutFormulaire(StatutCommande.PLANIFIEE))).isEmpty();
        assertThat(validator.validate(new ArretFormulaire(StatutArret.LIVRE, 1000))).isEmpty();
    }
}
