package com.mfish.erp.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.client.ClientService;
import com.mfish.erp.commande.CommandeApplicationService;
import com.mfish.erp.produit.ProduitService;
import com.mfish.erp.quartier.QuartierService;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.config.WebConfig;
import com.mfish.erp.web.mapper.MonnaieMapper;
import com.mfish.erp.web.mapper.VueMapper;
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

@WebMvcTest(CommandeWebController.class)
@Import({WebConfig.class, MonnaieMapper.class, VueMapper.class})
@WithMockUser(roles = "ADMIN")
class CommandeWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CommandeApplicationService commandeApplicationService;
    @MockitoBean
    private ClientService clientService;
    @MockitoBean
    private ProduitService produitService;
    @MockitoBean
    private QuartierService quartierService;
    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("liste les commandes")
    void liste_les_commandes() throws Exception {
        given(commandeApplicationService.rechercher(any(), any(), any(), any(), any()))
                .willReturn(Page.empty());
        given(clientService.lister()).willReturn(List.of());
        given(quartierService.lister()).willReturn(List.of());
        given(produitService.lister()).willReturn(List.of());

        mockMvc.perform(get("/commandes"))
                .andExpect(status().isOk())
                .andExpect(view().name("commandes/liste"));
    }

    @Test
    @DisplayName("retourne le fragment pour une requete htmx")
    void retourne_le_fragment_htmx() throws Exception {
        given(commandeApplicationService.rechercher(any(), any(), any(), any(), any()))
                .willReturn(Page.empty());
        given(clientService.lister()).willReturn(List.of());
        given(quartierService.lister()).willReturn(List.of());
        given(produitService.lister()).willReturn(List.of());

        mockMvc.perform(get("/commandes").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("commandes/fragments/liste"));
    }

    @Test
    @DisplayName("affiche le formulaire de creation")
    void affiche_formulaire_creation() throws Exception {
        given(clientService.lister()).willReturn(List.of());
        given(produitService.lister()).willReturn(List.of());

        mockMvc.perform(get("/commandes/nouvelle"))
                .andExpect(status().isOk())
                .andExpect(view().name("commandes/formulaire"));
    }

    @Test
    @DisplayName("refuse une commande sans ligne")
    void refuse_commande_sans_ligne() throws Exception {
        given(clientService.lister()).willReturn(List.of());
        given(produitService.lister()).willReturn(List.of());

        mockMvc.perform(post("/commandes")
                        .with(csrf())
                        .param("clientId", UUID.randomUUID().toString())
                        .param("modePaiement", "ESPECES"))
                .andExpect(status().isOk())
                .andExpect(view().name("commandes/formulaire"));
    }

    @Test
    @DisplayName("affiche le detail d'une commande")
    void affiche_detail_commande() throws Exception {
        final UUID id = UUID.randomUUID();
        given(commandeApplicationService.trouver(eq(id)))
                .willReturn(new com.mfish.erp.commande.CommandeDto(id, UUID.randomUUID(),
                        com.mfish.erp.commande.StatutCommande.A_PLANIFIER, null, 0,
                        com.mfish.erp.commande.ModePaiement.ESPECES,
                        com.mfish.erp.commande.StatutPaiement.NON_PAYE, 0, null,
                        com.mfish.erp.commande.SourceCommande.MANUEL, List.of()));
        given(clientService.lister()).willReturn(List.of());
        given(quartierService.lister()).willReturn(List.of());
        given(produitService.lister()).willReturn(List.of());

        mockMvc.perform(get("/commandes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("commandes/detail"));
    }

    @Test
    @DisplayName("change le statut d'une commande")
    void change_statut() throws Exception {
        final UUID id = UUID.randomUUID();
        mockMvc.perform(post("/commandes/{id}/statut", id).with(csrf()).param("statut", "PLANIFIEE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/commandes/" + id));
    }

    @Test
    @DisplayName("encaisse un paiement")
    void encaisse_paiement() throws Exception {
        final UUID id = UUID.randomUUID();
        mockMvc.perform(post("/commandes/{id}/paiements", id).with(csrf()).param("montant", "1000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/commandes/" + id));
    }

    @Test
    @DisplayName("annule une commande")
    void annule_commande() throws Exception {
        final UUID id = UUID.randomUUID();
        mockMvc.perform(post("/commandes/{id}/annulation", id).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/commandes/" + id));
    }
}
