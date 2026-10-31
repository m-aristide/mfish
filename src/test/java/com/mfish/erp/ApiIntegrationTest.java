package com.mfish.erp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("doit_parcourir_le_flux_api_de_bout_en_bout")
    void doit_parcourir_le_flux_api_de_bout_en_bout() throws Exception {
        final String token = obtenirJeton();

        final String quartierId = creerQuartier(token);
        final String produitId = creerProduit(token);
        final String clientId = creerClient(token, quartierId);

        mockMvc.perform(get("/api/clients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/commandes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + clientId + "\","
                                + "\"lignes\":[{\"produitId\":\"" + produitId + "\",\"quantite\":2}],"
                                + "\"modePaiement\":\"ESPECES\",\"source\":\"MANUEL\","
                                + "\"dateLivraisonSouhaitee\":\"2026-11-05\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montantTotal").value(2000));

        mockMvc.perform(get("/api/stats/ca")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("debut", "2026-11-01").param("fin", "2026-11-30"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stats/top-produits")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("debut", "2026-11-01").param("fin", "2026-11-30"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tableau-de-bord")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encaissement.encaisse").exists());
    }

    private String creerQuartier(final String token) throws Exception {
        final String corps = "{\"nom\":\"TestQuartier9\",\"zone\":\"Nord\",\"latitude\":5.35,"
                + "\"longitude\":-3.97,\"ordreDefaut\":1,\"actif\":true}";
        final String body = mockMvc.perform(post("/api/quartiers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String creerProduit(final String token) throws Exception {
        final String body = mockMvc.perform(post("/api/produits")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"TestProduit\",\"prix\":1000,\"unite\":\"plat\",\"actif\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String creerClient(final String token, final String quartierId) throws Exception {
        final String corps = "{\"nom\":\"TestClient\",\"telephone\":\"+2250799999901\","
                + "\"quartierId\":\"" + quartierId + "\"}";
        final String body = mockMvc.perform(post("/api/clients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
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
