package com.mfish.erp.web.controller;

import com.mfish.erp.connaissance.ConnaissanceService;
import com.mfish.erp.connaissance.TestIaService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.ConnaissanceFormulaire;
import com.mfish.erp.web.dto.QuestionTestFormulaire;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ecran « Base de connaissance » : editeur Markdown, historique des versions, test IA.
 */
@Controller
@RequestMapping("/connaissance")
public class ConnaissanceWebController {

    private final ConnaissanceService connaissanceService;
    private final TestIaService testIaService;
    private final FlashBuilder flashBuilder;

    public ConnaissanceWebController(final ConnaissanceService connaissanceService,
                                     final TestIaService testIaService,
                                     final FlashBuilder flashBuilder) {
        this.connaissanceService = connaissanceService;
        this.testIaService = testIaService;
        this.flashBuilder = flashBuilder;
    }

    @GetMapping
    public String editeur(final Model model) {
        preparer(model);
        return "connaissance/editeur";
    }

    @PostMapping
    public String enregistrer(@Valid @ModelAttribute("formulaire") final ConnaissanceFormulaire formulaire,
                              final BindingResult bindingResult, final Model model,
                              final RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            connaissanceService.enregistrer(formulaire.contenu());
            redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.enregistre"));
            return "redirect:/connaissance";
        }
        preparer(model);
        return "connaissance/editeur";
    }

    @PostMapping("/versions/{id}/restaurer")
    public String restaurer(@PathVariable final UUID id, final RedirectAttributes redirectAttributes) {
        connaissanceService.restaurer(id);
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.enregistre"));
        return "redirect:/connaissance";
    }

    @PostMapping("/test")
    public String tester(@Valid @ModelAttribute("questionForm") final QuestionTestFormulaire formulaire,
                         final BindingResult bindingResult, final Model model) {
        if (!bindingResult.hasErrors()) {
            model.addAttribute("reponse", testIaService.tester(formulaire.question()));
        }
        preparer(model);
        return "connaissance/editeur";
    }

    private void preparer(final Model model) {
        model.addAttribute("contenu", connaissanceService.contenuCourant());
        model.addAttribute("versions", connaissanceService.versions());
        if (!model.containsAttribute("formulaire")) {
            model.addAttribute("formulaire", new ConnaissanceFormulaire(""));
        }
        if (!model.containsAttribute("questionForm")) {
            model.addAttribute("questionForm", new QuestionTestFormulaire(""));
        }
        model.addAttribute("pageActive", "connaissance");
    }
}
