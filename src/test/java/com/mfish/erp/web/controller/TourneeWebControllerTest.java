package com.mfish.erp.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.client.ClientService;
import com.mfish.erp.quartier.QuartierService;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.tournee.StatutTournee;
import com.mfish.erp.tournee.Tournee;
import com.mfish.erp.tournee.TourneeService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.config.WebConfig;
import com.mfish.erp.web.mapper.MonnaieMapper;
import com.mfish.erp.web.mapper.VueMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TourneeWebController.class)
@Import({WebConfig.class, MonnaieMapper.class, VueMapper.class})
@WithMockUser(roles = "ADMIN")
class TourneeWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TourneeService tourneeService;
    @MockitoBean
    private ClientService clientService;
    @MockitoBean
    private QuartierService quartierService;
    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("affiche la tournee du jour")
    void affiche_la_tournee() throws Exception {
        given(tourneeService.tourneeDuJour())
                .willReturn(new Tournee(LocalDate.now(), StatutTournee.BROUILLON));
        given(tourneeService.arrets(any())).willReturn(List.of());
        given(clientService.lister()).willReturn(List.of());
        given(quartierService.lister()).willReturn(List.of());

        mockMvc.perform(get("/tournee"))
                .andExpect(status().isOk())
                .andExpect(view().name("tournee/liste"));
    }

    @Test
    @DisplayName("recalcule la tournee en htmx et renvoie le fragment")
    void recalcule_et_renvoie_fragment() throws Exception {
        given(tourneeService.tourneeDuJour())
                .willReturn(new Tournee(LocalDate.now(), StatutTournee.BROUILLON));
        given(tourneeService.calculer()).willReturn(new Tournee(LocalDate.now(), StatutTournee.VALIDEE));
        given(tourneeService.arrets(any())).willReturn(List.of());
        given(clientService.lister()).willReturn(List.of());
        given(quartierService.lister()).willReturn(List.of());

        mockMvc.perform(post("/tournee/calculer").with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("tournee/fragments/liste"));
    }

    @Test
    @DisplayName("marque un arret en htmx et renvoie le fragment")
    void marque_un_arret() throws Exception {
        given(tourneeService.tourneeDuJour())
                .willReturn(new Tournee(LocalDate.now(), StatutTournee.BROUILLON));
        given(tourneeService.arrets(any())).willReturn(List.of());
        given(clientService.lister()).willReturn(List.of());
        given(quartierService.lister()).willReturn(List.of());

        mockMvc.perform(post("/tournee/arrets/{id}", java.util.UUID.randomUUID())
                        .with(csrf()).header("HX-Request", "true")
                        .param("statut", "LIVRE").param("montant", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("tournee/fragments/liste"));
    }
}
