package com.mfish.erp.web.controller;

import com.mfish.erp.produit.ProduitRequest;
import com.mfish.erp.produit.ProduitService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.ProduitFormulaire;
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
 * Ecran « Produits / catalogue » : liste, creation, modification, activation/desactivation.
 */
@Controller
@RequestMapping("/produits")
public class ProduitWebController {

    private final ProduitService produitService;
    private final FlashBuilder flashBuilder;

    public ProduitWebController(final ProduitService produitService, final FlashBuilder flashBuilder) {
        this.produitService = produitService;
        this.flashBuilder = flashBuilder;
    }

    @GetMapping
    public String lister(final Model model) {
        model.addAttribute("produits", produitService.lister());
        model.addAttribute("pageActive", "produits");
        return "produits/liste";
    }

    @GetMapping("/nouveau")
    public String formulaireCreation(final Model model) {
        preparerFormulaire(model, new ProduitFormulaire(null, 0, null, true));
        return "produits/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("formulaire") final ProduitFormulaire formulaire,
                        final BindingResult bindingResult, final Model model,
                        final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, formulaire);
            return "produits/formulaire";
        }
        produitService.creer(requete(formulaire));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.cree"));
        return "redirect:/produits";
    }

    @GetMapping("/{id}/modifier")
    public String formulaireModification(@PathVariable final UUID id, final Model model) {
        final var produit = produitService.trouver(id);
        preparerFormulaire(model, new ProduitFormulaire(produit.nom(), produit.prix(),
                produit.unite(), produit.actif()));
        model.addAttribute("produitId", id);
        return "produits/formulaire";
    }

    @PostMapping("/{id}")
    public String modifier(@PathVariable final UUID id,
                           @Valid @ModelAttribute("formulaire") final ProduitFormulaire formulaire,
                           final BindingResult bindingResult, final Model model,
                           final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, formulaire);
            model.addAttribute("produitId", id);
            return "produits/formulaire";
        }
        produitService.modifier(id, requete(formulaire));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.modifie"));
        return "redirect:/produits";
    }

    @PostMapping("/{id}/basculer")
    public String basculer(@PathVariable final UUID id, final RedirectAttributes redirectAttributes) {
        final var produit = produitService.trouver(id);
        produitService.modifier(id, new ProduitRequest(produit.nom(), produit.prix(),
                produit.unite(), !produit.actif()));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.modifie"));
        return "redirect:/produits";
    }

    private ProduitRequest requete(final ProduitFormulaire formulaire) {
        return new ProduitRequest(formulaire.nom(), formulaire.prix(), formulaire.unite(),
                formulaire.actif());
    }

    private void preparerFormulaire(final Model model, final ProduitFormulaire formulaire) {
        model.addAttribute("formulaire", formulaire);
        model.addAttribute("pageActive", "produits");
    }
}
