package com.mfish.erp;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Validation de bout en bout sur PostgreSQL reel (Testcontainers) : demarrage, connexion
 * par formulaire et acces aux ecrans. Utilise les migrations de production (db/migration)
 * et la validation Hibernate (ddl-auto=validate).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-pg")
@TestPropertySource(properties = "app.tests.use-testcontainers=true")
@Import(TestcontainersConfiguration.class)
class PostgresWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("la page de connexion repond sur PostgreSQL")
    void page_connexion_repond() throws Exception {
        mockMvc.perform(get("/connexion")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("connexion par formulaire reussie sur PostgreSQL")
    void connexion_reussie() throws Exception {
        mockMvc.perform(post("/connexion")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("les ecrans admin repondent sur PostgreSQL")
    void ecrans_admin_repondent() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/produits")).andExpect(status().isOk());
        mockMvc.perform(get("/commandes")).andExpect(status().isOk());
        mockMvc.perform(get("/tournee")).andExpect(status().isOk());
        mockMvc.perform(get("/clients")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LIVREUR")
    @DisplayName("un livreur est refuse sur un ecran admin")
    void livreur_refuse_ecran_admin() throws Exception {
        mockMvc.perform(get("/produits")).andExpect(status().isForbidden());
        mockMvc.perform(get("/tournee")).andExpect(status().isOk());
    }
}
