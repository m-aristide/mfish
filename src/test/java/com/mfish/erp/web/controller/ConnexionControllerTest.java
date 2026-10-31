package com.mfish.erp.web.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.FlashMessage;
import com.mfish.erp.securite.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConnexionController.class)
@WithMockUser
class ConnexionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("affiche la page de connexion")
    void affiche_la_page_de_connexion() throws Exception {
        mockMvc.perform(get("/connexion"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"));
    }

    @Test
    @DisplayName("affiche un message d'erreur sur identifiants invalides")
    void affiche_message_erreur() throws Exception {
        given(flashBuilder.erreur(anyString())).willReturn(FlashMessage.erreur("Identifiant incorrect."));
        mockMvc.perform(get("/connexion").param("erreur", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"))
                .andExpect(model().attributeExists("flash"));
    }
}
