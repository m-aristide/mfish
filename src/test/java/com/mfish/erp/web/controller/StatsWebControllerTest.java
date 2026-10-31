package com.mfish.erp.web.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.securite.JwtService;
import com.mfish.erp.stats.StatsService;
import com.mfish.erp.web.config.WebConfig;
import com.mfish.erp.web.mapper.MonnaieMapper;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StatsWebController.class)
@Import({WebConfig.class, MonnaieMapper.class})
@WithMockUser(roles = "ADMIN")
class StatsWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private StatsService statsService;
    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("affiche les statistiques")
    void affiche_statistiques() throws Exception {
        given(statsService.caParPeriode(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(0L);
        given(statsService.nbCommandes(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(0L);
        given(statsService.tauxLivraison(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(0.0);
        given(statsService.topProduits(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt())).willReturn(List.of());
        given(statsService.caParQuartier(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of());

        mockMvc.perform(get("/statistiques").param("debut", "2026-11-01").param("fin", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("statistiques/index"));
    }

    @Test
    @DisplayName("exporte les statistiques en CSV")
    void exporte_csv() throws Exception {
        given(statsService.caParPeriode(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(0L);
        given(statsService.nbCommandes(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(0L);
        given(statsService.tauxLivraison(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(0.0);
        given(statsService.topProduits(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt())).willReturn(List.of());
        given(statsService.caParQuartier(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of());

        mockMvc.perform(get("/statistiques/export").param("debut", "2026-11-01").param("fin", "2026-11-30"))
                .andExpect(status().isOk());
    }
}
