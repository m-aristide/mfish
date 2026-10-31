package com.mfish.erp.web.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mfish.erp.config.ChatwootProperties;
import com.mfish.erp.conversation.ConversationService;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.securite.JwtService;
import com.mfish.erp.web.FlashBuilder;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConversationWebController.class)
@WithMockUser(roles = "ADMIN")
class ConversationWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private ConversationService conversationService;
    @MockitoBean
    private ChatwootProperties chatwootProperties;
    @MockitoBean
    private FlashBuilder flashBuilder;

    @Test
    @DisplayName("liste les conversations")
    void liste_les_conversations() throws Exception {
        given(conversationService.lister()).willReturn(List.of());
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isOk())
                .andExpect(view().name("conversations/liste"));
    }

    @Test
    @DisplayName("bascule une conversation en mode humain")
    void bascule_en_mode_humain() throws Exception {
        mockMvc.perform(post("/conversations/{id}/mode", java.util.UUID.randomUUID())
                        .with(csrf()).param("mode", ModeConversation.HUMAIN.name()))
                .andExpect(status().is3xxRedirection());
    }
}
