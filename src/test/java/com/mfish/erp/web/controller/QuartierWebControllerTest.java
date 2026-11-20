package com.mfish.erp.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.quartier.QuartierDto;
import com.mfish.erp.quartier.QuartierRequest;
import com.mfish.erp.quartier.QuartierService;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.config.WebConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuartierWebController.class)
@Import(WebConfig.class)
@WithMockUser(roles = "ADMIN")
class QuartierWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private QuartierService quartierService;
    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("liste les quartiers tries par ordre de tournee")
    void liste_les_quartiers() throws Exception {
        given(quartierService.lister()).willReturn(List.of(
                new QuartierDto(UUID.randomUUID(), "Cocody", "Est", 5.35, -3.99, 2, true),
                new QuartierDto(UUID.randomUUID(), "Adjame", "Nord", null, null, 1, true)));

        mockMvc.perform(get("/quartiers"))
                .andExpect(status().isOk())
                .andExpect(view().name("quartiers/liste"))
                .andExpect(model().attributeExists("quartiers"));
    }

    @Test
    @DisplayName("affiche le formulaire de creation")
    void affiche_formulaire_creation() throws Exception {
        mockMvc.perform(get("/quartiers/nouveau"))
                .andExpect(status().isOk())
                .andExpect(view().name("quartiers/formulaire"))
                .andExpect(model().attributeExists("formulaire"));
    }

    @Test
    @DisplayName("cree un quartier valide puis redirige vers la liste")
    void cree_quartier_valide() throws Exception {
        given(quartierService.nomDejaUtilise("Cocody", null)).willReturn(false);

        mockMvc.perform(post("/quartiers")
                        .with(csrf())
                        .param("nom", "Cocody")
                        .param("zone", "Est")
                        .param("latitude", "5.35")
                        .param("longitude", "-3.99")
                        .param("ordreDefaut", "2")
                        .param("actif", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quartiers"));

        then(quartierService).should()
                .creer(new QuartierRequest("Cocody", "Est", 5.35, -3.99, 2, true));
    }

    @Test
    @DisplayName("refuse un quartier sans nom")
    void refuse_quartier_sans_nom() throws Exception {
        mockMvc.perform(post("/quartiers")
                        .with(csrf())
                        .param("nom", "")
                        .param("ordreDefaut", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("quartiers/formulaire"))
                .andExpect(model().attributeHasFieldErrors("formulaire", "nom"));

        then(quartierService).should(never()).creer(any());
    }

    @Test
    @DisplayName("refuse un nom de quartier deja utilise")
    void refuse_nom_deja_utilise() throws Exception {
        given(quartierService.nomDejaUtilise("Cocody", null)).willReturn(true);

        mockMvc.perform(post("/quartiers")
                        .with(csrf())
                        .param("nom", "Cocody")
                        .param("ordreDefaut", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("quartiers/formulaire"))
                .andExpect(model().attributeHasFieldErrors("formulaire", "nom"));

        then(quartierService).should(never()).creer(any());
    }

    @Test
    @DisplayName("refuse des coordonnees hors bornes")
    void refuse_coordonnees_hors_bornes() throws Exception {
        mockMvc.perform(post("/quartiers")
                        .with(csrf())
                        .param("nom", "Cocody")
                        .param("latitude", "120")
                        .param("longitude", "-200")
                        .param("ordreDefaut", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("quartiers/formulaire"))
                .andExpect(model().attributeHasFieldErrors("formulaire", "latitude", "longitude"));

        then(quartierService).should(never()).creer(any());
    }

    @Test
    @DisplayName("affiche le formulaire de modification pre-rempli")
    void affiche_formulaire_modification() throws Exception {
        final UUID id = UUID.randomUUID();
        given(quartierService.trouver(id))
                .willReturn(new QuartierDto(id, "Cocody", "Est", 5.35, -3.99, 2, true));

        mockMvc.perform(get("/quartiers/{id}/modifier", id))
                .andExpect(status().isOk())
                .andExpect(view().name("quartiers/formulaire"))
                .andExpect(model().attribute("quartierId", id));
    }

    @Test
    @DisplayName("modifie un quartier puis redirige vers la liste")
    void modifie_quartier() throws Exception {
        final UUID id = UUID.randomUUID();
        given(quartierService.nomDejaUtilise("Cocody", id)).willReturn(false);

        mockMvc.perform(post("/quartiers/{id}", id)
                        .with(csrf())
                        .param("nom", "Cocody")
                        .param("zone", "Est")
                        .param("ordreDefaut", "2")
                        .param("actif", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quartiers"));

        then(quartierService).should()
                .modifier(id, new QuartierRequest("Cocody", "Est", null, null, 2, true));
    }

    @Test
    @DisplayName("bascule l'activation d'un quartier")
    void bascule_activation() throws Exception {
        final UUID id = UUID.randomUUID();
        given(quartierService.trouver(id))
                .willReturn(new QuartierDto(id, "Cocody", "Est", null, null, 2, true));

        mockMvc.perform(post("/quartiers/{id}/basculer", id).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quartiers"));

        then(quartierService).should()
                .modifier(id, new QuartierRequest("Cocody", "Est", null, null, 2, false));
    }
}
