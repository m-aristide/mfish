package com.mfish.erp.web.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("anonyme redirige vers la connexion")
    void anonyme_redirige_vers_connexion() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
    }

    @Test
    @DisplayName("la page de connexion est accessible")
    void page_connexion_accessible() throws Exception {
        mockMvc.perform(get("/connexion"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("connexion avec de bons identifiants redirige vers le tableau de bord")
    void connexion_reussie() throws Exception {
        mockMvc.perform(post("/connexion")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST sans jeton CSRF refuse")
    void post_sans_csrf_refuse() throws Exception {
        mockMvc.perform(post("/commandes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("livreur refuse sur une route admin")
    void livreur_refuse_route_admin() throws Exception {
        mockMvc.perform(get("/produits").with(user("livreur").roles("LIVREUR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin accede a une route admin")
    void admin_accede_route_admin() throws Exception {
        mockMvc.perform(get("/produits").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("les en-tetes de securite sont presents")
    void en_tetes_securite_presents() throws Exception {
        mockMvc.perform(get("/connexion"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Referrer-Policy"));
    }
}
