package com.mfish.erp.web.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.connaissance.ConnaissanceService;
import com.mfish.erp.connaissance.TestIaService;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.web.FlashBuilder;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConnaissanceWebController.class)
@WithMockUser(roles = "ADMIN")
class ConnaissanceWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private ConnaissanceService connaissanceService;
    @MockitoBean
    private TestIaService testIaService;
    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("affiche l'editeur de connaissance")
    void affiche_editeur() throws Exception {
        given(connaissanceService.contenuCourant()).willReturn("");
        given(connaissanceService.versions()).willReturn(List.of());

        mockMvc.perform(get("/connaissance"))
                .andExpect(status().isOk())
                .andExpect(view().name("connaissance/editeur"));
    }

    @Test
    @DisplayName("refuse une connaissance vide")
    void refuse_connaissance_vide() throws Exception {
        given(connaissanceService.contenuCourant()).willReturn("");
        given(connaissanceService.versions()).willReturn(List.of());

        mockMvc.perform(post("/connaissance").with(csrf()).param("contenu", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("connaissance/editeur"));
    }

    @Test
    @DisplayName("teste l'IA")
    void teste_l_ia() throws Exception {
        given(connaissanceService.contenuCourant()).willReturn("");
        given(connaissanceService.versions()).willReturn(List.of());
        given(testIaService.tester("Bonjour")).willReturn("Reponse");

        mockMvc.perform(post("/connaissance/test").with(csrf()).param("question", "Bonjour"))
                .andExpect(status().isOk())
                .andExpect(view().name("connaissance/editeur"));
    }
}
