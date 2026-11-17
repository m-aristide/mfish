package com.mfish.erp.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.client.ClientDto;
import com.mfish.erp.client.ClientService;
import com.mfish.erp.commande.CommandeApplicationService;
import com.mfish.erp.quartier.QuartierService;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.config.WebConfig;
import com.mfish.erp.web.mapper.MonnaieMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientWebController.class)
@Import({WebConfig.class, MonnaieMapper.class})
@WithMockUser(roles = "ADMIN")
class ClientWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ClientService clientService;
    @MockitoBean
    private QuartierService quartierService;
    @MockitoBean
    private CommandeApplicationService commandeApplicationService;
    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("liste les clients")
    void liste_les_clients() throws Exception {
        given(clientService.rechercher(any(), any())).willReturn(Page.empty());
        given(quartierService.lister()).willReturn(List.of());

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/liste"));
    }

    @Test
    @DisplayName("affiche le formulaire de creation")
    void affiche_formulaire_creation() throws Exception {
        given(quartierService.lister()).willReturn(List.of());

        mockMvc.perform(get("/clients/nouveau"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/formulaire"));
    }

    @Test
    @DisplayName("refuse un client sans nom ni telephone")
    void refuse_client_invalide() throws Exception {
        given(quartierService.lister()).willReturn(List.of());

        mockMvc.perform(post("/clients")
                        .with(csrf())
                        .param("nom", "")
                        .param("telephone", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/formulaire"));
    }

    @Test
    @DisplayName("affiche la fiche d'un client")
    void affiche_fiche_client() throws Exception {
        final UUID id = UUID.randomUUID();
        given(clientService.trouver(id)).willReturn(new ClientDto(id, "Awa", "+2250700000001",
                null, null, null, null, null, null, false));
        given(commandeApplicationService.listerParClient(id)).willReturn(List.of());

        mockMvc.perform(get("/clients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/detail"));
    }

    @Test
    @DisplayName("archive un client")
    void archive_client() throws Exception {
        final UUID id = UUID.randomUUID();
        mockMvc.perform(post("/clients/{id}/archiver", id).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients"));
    }
}
