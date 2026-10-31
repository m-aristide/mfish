package com.mfish.erp.web.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.produit.ProduitService;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.config.WebConfig;
import com.mfish.erp.web.mapper.MonnaieMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProduitWebController.class)
@Import({WebConfig.class, MonnaieMapper.class})
@WithMockUser(roles = "ADMIN")
class ProduitWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private ProduitService produitService;
    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("liste les produits")
    void liste_les_produits() throws Exception {
        given(produitService.lister()).willReturn(List.of());
        mockMvc.perform(get("/produits"))
                .andExpect(status().isOk())
                .andExpect(view().name("produits/liste"));
    }

    @Test
    @DisplayName("affiche le formulaire de creation")
    void affiche_formulaire_creation() throws Exception {
        mockMvc.perform(get("/produits/nouveau"))
                .andExpect(status().isOk())
                .andExpect(view().name("produits/formulaire"));
    }

    @Test
    @DisplayName("refuse un produit sans nom")
    void refuse_produit_sans_nom() throws Exception {
        mockMvc.perform(post("/produits").with(csrf()).param("nom", "").param("prix", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("produits/formulaire"));
    }
}
