package com.mfish.erp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mfish.erp.commande.CommandeInvalideException;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commande.SurpaiementException;
import com.mfish.erp.commande.TransitionStatutInvalideException;
import com.mfish.erp.commun.NotFoundException;
import com.mfish.erp.securite.IdentifiantsInvalidesException;
import com.mfish.erp.securite.TropDeTentativesException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("doit_gerer_la_ressource_introuvable")
    void doit_gerer_la_ressource_introuvable() {
        assertThat(handler.notFound(new NotFoundException("x")).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("doit_gerer_les_identifiants_invalides")
    void doit_gerer_les_identifiants_invalides() {
        assertThat(handler.identifiantsInvalides(new IdentifiantsInvalidesException()).getStatus())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("doit_gerer_les_trop_de_tentatives")
    void doit_gerer_les_trop_de_tentatives() {
        assertThat(handler.tropDeTentatives(new TropDeTentativesException()).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("doit_gerer_la_commande_invalide")
    void doit_gerer_la_commande_invalide() {
        assertThat(handler.commandeInvalide(new CommandeInvalideException("x")).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("doit_gerer_les_conflits")
    void doit_gerer_les_conflits() {
        assertThat(handler.conflit(new SurpaiementException(100, 200)).getStatus()).isEqualTo(409);
        assertThat(handler.conflit(new TransitionStatutInvalideException(
                StatutCommande.A_PLANIFIER, StatutCommande.LIVREE)).getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("doit_gerer_la_violation_de_contrainte")
    void doit_gerer_la_violation_de_contrainte() {
        assertThat(handler.violationContrainte(new DataIntegrityViolationException("x")).getStatus())
                .isEqualTo(409);
    }

    @Test
    @DisplayName("doit_gerer_la_validation")
    void doit_gerer_la_validation() {
        final MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        final BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        assertThat(handler.validation(ex).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("doit_gerer_l_erreur_interne")
    void doit_gerer_l_erreur_interne() {
        assertThat(handler.interne(new RuntimeException("x")).getStatus()).isEqualTo(500);
    }
}
