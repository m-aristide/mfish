package com.mfish.erp.simulateur;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.securite.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifie que la page du simulateur est servie et expose le jeton CSRF attendu par le script
 * ({@code app-simulateur.js}) : sans lui, le POST d'un message est refuse (403).
 */
@WebMvcTest(value = SimulateurWebController.class, properties = "app.simulateur.active=true")
class SimulateurWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("sert la page avec un jeton CSRF non vide")
    void sert_la_page_avec_jeton_csrf() throws Exception {
        mockMvc.perform(get("/simulateur"))
                .andExpect(status().isOk())
                .andExpect(view().name("simulateur/index"))
                .andExpect(model().attributeExists("csrf"))
                .andExpect(content().string(containsString("name=\"_csrf\" content=\"")));
    }
}
