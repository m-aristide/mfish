package com.mfish.erp.securite;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.mfish.erp.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("doit_retourner_un_jeton_pour_de_bons_identifiants")
    void doit_retourner_un_jeton_pour_de_bons_identifiants() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("doit_refuser_de_mauvais_identifiants")
    void doit_refuser_de_mauvais_identifiants() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mauvais\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("doit_refuser_route_protegee_sans_authentification")
    void doit_refuser_route_protegee_sans_authentification() throws Exception {
        mockMvc.perform(get("/api/clients")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("doit_autoriser_route_protegee_avec_jeton_valide")
    void doit_autoriser_route_protegee_avec_jeton_valide() throws Exception {
        final String token = obtenirJeton();

        mockMvc.perform(get("/api/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("doit_refuser_route_protegee_avec_jeton_invalide")
    void doit_refuser_route_protegee_avec_jeton_invalide() throws Exception {
        mockMvc.perform(get("/api/clients").header(HttpHeaders.AUTHORIZATION, "Bearer jeton.invalide"))
                .andExpect(status().isUnauthorized());
    }

    private String obtenirJeton() throws Exception {
        final String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
