package com.mfish.erp.whatsapp.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

class WhatsAppErreurTraducteurTest {

    private final WhatsAppErreurTraducteur traducteur = new WhatsAppErreurTraducteur();

    @Test
    @DisplayName("doit_traduire_le_code_190_en_jeton_invalide")
    void doit_traduire_le_code_190_en_jeton_invalide() {
        assertThat(traduire(190)).isInstanceOf(WhatsAppTokenInvalideException.class);
    }

    @Test
    @DisplayName("doit_traduire_le_code_131030_en_destinataire_invalide")
    void doit_traduire_le_code_131030_en_destinataire_invalide() {
        assertThat(traduire(131030)).isInstanceOf(WhatsAppDestinataireInvalideException.class);
    }

    @Test
    @DisplayName("doit_traduire_le_code_131047_en_hors_fenetre")
    void doit_traduire_le_code_131047_en_hors_fenetre() {
        assertThat(traduire(131047)).isInstanceOf(WhatsAppHorsFenetreException.class);
    }

    @Test
    @DisplayName("doit_traduire_le_code_132000_en_template_non_approuve")
    void doit_traduire_le_code_132000_en_template_non_approuve() {
        assertThat(traduire(132000)).isInstanceOf(WhatsAppTemplateNonApprouveException.class);
    }

    @Test
    @DisplayName("doit_traduire_le_code_131026_en_limite_de_debit")
    void doit_traduire_le_code_131026_en_limite_de_debit() {
        assertThat(traduire(131026)).isInstanceOf(WhatsAppLimiteDebitException.class);
    }

    @Test
    @DisplayName("doit_traduire_le_code_131049_en_numero_non_joignable")
    void doit_traduire_le_code_131049_en_numero_non_joignable() {
        assertThat(traduire(131049)).isInstanceOf(WhatsAppNumeroNonJoignableException.class);
    }

    @Test
    @DisplayName("doit_retourner_une_exception_generique_pour_code_inconnu")
    void doit_retourner_une_exception_generique_pour_code_inconnu() {
        assertThat(traduire(999)).isInstanceOf(WhatsAppException.class);
    }

    private WhatsAppException traduire(final int code) {
        final String corps = "{\"error\":{\"message\":\"erreur\",\"code\":" + code + "}}";
        final RestClientResponseException e = new RestClientResponseException(
                "Bad Request", 400, "Bad Request", new HttpHeaders(),
                corps.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return traducteur.traduire(e);
    }
}
