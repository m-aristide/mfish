package com.mfish.erp.web.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.parametre.ParametreService;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.utilisateur.UtilisateurService;
import com.mfish.erp.web.FlashBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ParametreWebController.class)
@WithMockUser(roles = "ADMIN")
class ParametreWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private ParametreService parametreService;
    @MockitoBean
    private UtilisateurService utilisateurService;
    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("affiche les parametres")
    void affiche_parametres() throws Exception {
        given(parametreService.toutesLesValeurs()).willReturn(Map.of());
        given(utilisateurService.lister()).willReturn(List.of());

        mockMvc.perform(get("/parametres"))
                .andExpect(status().isOk())
                .andExpect(view().name("parametres/index"));
    }

    @Test
    @DisplayName("enregistre les parametres du commerce")
    void enregistre_parametres() throws Exception {
        mockMvc.perform(post("/parametres").with(csrf()).param("nomCommerce", "Boutique"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("refuse un utilisateur sans mot de passe")
    void refuse_utilisateur_sans_mot_de_passe() throws Exception {
        given(parametreService.toutesLesValeurs()).willReturn(Map.of());
        given(utilisateurService.lister()).willReturn(List.of());

        mockMvc.perform(post("/parametres/utilisateurs").with(csrf())
                        .param("username", "livreur")
                        .param("motDePasse", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("parametres/index"));
    }
}
