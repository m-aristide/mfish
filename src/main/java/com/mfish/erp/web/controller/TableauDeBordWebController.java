package com.mfish.erp.web.controller;

import com.mfish.erp.commande.CommandeDto;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.conversation.ConversationService;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.stats.TableauDeBordService;
import com.mfish.erp.web.dto.TableauDeBordVue;
import com.mfish.erp.web.mapper.MonnaieMapper;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Ecran « Tableau de bord » du commercant (cartes + raccourcis), avec rafraichissement htmx.
 */
@Controller
public class TableauDeBordWebController extends AbstractWebController {

    private final TableauDeBordService tableauDeBordService;
    private final ConversationService conversationService;
    private final MonnaieMapper monnaieMapper;

    public TableauDeBordWebController(final TableauDeBordService tableauDeBordService,
                                      final ConversationService conversationService,
                                      final MonnaieMapper monnaieMapper) {
        this.tableauDeBordService = tableauDeBordService;
        this.conversationService = conversationService;
        this.monnaieMapper = monnaieMapper;
    }

    @GetMapping("/")
    public String tableauDeBord(final Model model) {
        model.addAttribute("vue", construireVue());
        model.addAttribute("pageActive", "tableauDeBord");
        return "tableau-de-bord";
    }

    @GetMapping("/tableau-de-bord/cartes")
    public String cartes(final Model model) {
        model.addAttribute("vue", construireVue());
        return "tableau-de-bord/fragments/cartes";
    }

    private TableauDeBordVue construireVue() {
        final List<CommandeDto> livraisons = tableauDeBordService.livraisonsDuJour();
        final long commandesDuJour = livraisons.size();
        final long livrees = livraisons.stream()
                .filter(commande -> commande.statut() == StatutCommande.LIVREE).count();
        final long aLivrer = commandesDuJour - livrees;
        final long aEncaisser = tableauDeBordService.encaisseRestant().restant();
        final long conversations = conversationService.lister().stream()
                .filter(conversation -> conversation.mode() == ModeConversation.HUMAIN).count();
        return new TableauDeBordVue(commandesDuJour, aLivrer, livrees, aEncaisser,
                monnaieMapper.formater(aEncaisser), conversations);
    }
}
