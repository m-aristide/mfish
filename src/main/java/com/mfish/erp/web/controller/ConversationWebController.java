package com.mfish.erp.web.controller;

import com.mfish.erp.config.ChatwootProperties;
import com.mfish.erp.conversation.ConversationService;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.web.FlashBuilder;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ecran « Conversations WhatsApp » : liste (IA/Humain), detail, bascule de mode.
 */
@Controller
@RequestMapping("/conversations")
public class ConversationWebController {

    private final ConversationService conversationService;
    private final ChatwootProperties chatwootProperties;
    private final FlashBuilder flashBuilder;

    public ConversationWebController(final ConversationService conversationService,
                                     final ChatwootProperties chatwootProperties,
                                     final FlashBuilder flashBuilder) {
        this.conversationService = conversationService;
        this.chatwootProperties = chatwootProperties;
        this.flashBuilder = flashBuilder;
    }

    @GetMapping
    public String lister(final Model model) {
        model.addAttribute("conversations", conversationService.lister());
        model.addAttribute("pageActive", "conversations");
        return "conversations/liste";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable final UUID id, final Model model) {
        model.addAttribute("conversation", conversationService.trouver(id));
        model.addAttribute("chatwootBaseUrl", chatwootProperties.baseUrl());
        model.addAttribute("pageActive", "conversations");
        return "conversations/detail";
    }

    @PostMapping("/{id}/mode")
    public String basculer(@PathVariable final UUID id,
                           @RequestParam final ModeConversation mode,
                           final RedirectAttributes redirectAttributes) {
        conversationService.basculer(id, mode);
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.enregistre"));
        return "redirect:/conversations/" + id;
    }
}
