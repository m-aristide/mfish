package com.mfish.erp.web.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.conversation.ConversationService;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.stats.EncaissementDto;
import com.mfish.erp.stats.TableauDeBordService;
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

@WebMvcTest(TableauDeBordWebController.class)
@Import({WebConfig.class, MonnaieMapper.class})
@WithMockUser(roles = "ADMIN")
class TableauDeBordWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private TableauDeBordService tableauDeBordService;
    @MockitoBean
    private ConversationService conversationService;

    @Test
    @DisplayName("affiche le tableau de bord")
    void affiche_le_tableau_de_bord() throws Exception {
        given(tableauDeBordService.livraisonsDuJour()).willReturn(List.of());
        given(tableauDeBordService.encaisseRestant()).willReturn(new EncaissementDto(0, 0));
        given(conversationService.lister()).willReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("tableau-de-bord"));
    }

    @Test
    @DisplayName("affiche le fragment des cartes")
    void affiche_le_fragment_cartes() throws Exception {
        given(tableauDeBordService.livraisonsDuJour()).willReturn(List.of());
        given(tableauDeBordService.encaisseRestant()).willReturn(new EncaissementDto(0, 0));
        given(conversationService.lister()).willReturn(List.of());

        mockMvc.perform(get("/tableau-de-bord/cartes"))
                .andExpect(status().isOk())
                .andExpect(view().name("tableau-de-bord/fragments/cartes"));
    }
}
