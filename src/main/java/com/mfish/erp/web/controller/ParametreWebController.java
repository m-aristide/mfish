package com.mfish.erp.web.controller;

import com.mfish.erp.parametre.ParametreService;
import com.mfish.erp.utilisateur.UtilisateurService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.UtilisateurFormulaire;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ecran « Parametres » : infos du commerce + gestion des utilisateurs et roles.
 */
@Controller
@RequestMapping("/parametres")
public class ParametreWebController {

    private final ParametreService parametreService;
    private final UtilisateurService utilisateurService;
    private final FlashBuilder flashBuilder;

    public ParametreWebController(final ParametreService parametreService,
                                  final UtilisateurService utilisateurService,
                                  final FlashBuilder flashBuilder) {
        this.parametreService = parametreService;
        this.utilisateurService = utilisateurService;
        this.flashBuilder = flashBuilder;
    }

    @GetMapping
    public String parametres(final Model model) {
        preparer(model);
        return "parametres/index";
    }

    @PostMapping
    public String enregistrer(@RequestParam(required = false) final String nomCommerce,
                              @RequestParam(required = false) final String telephone,
                              @RequestParam(required = false) final String horaires,
                              @RequestParam(required = false) final String fraisLivraison,
                              final RedirectAttributes redirectAttributes) {
        enregistrerParametre("nom_commerce", nomCommerce);
        enregistrerParametre("telephone", telephone);
        enregistrerParametre("horaires", horaires);
        enregistrerParametre("frais_livraison", fraisLivraison);
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.enregistre"));
        return "redirect:/parametres";
    }

    @PostMapping("/utilisateurs")
    public String creerUtilisateur(@Valid @ModelAttribute("formulaire") final UtilisateurFormulaire formulaire,
                                   final BindingResult bindingResult, final Model model,
                                   final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preparer(model);
            return "parametres/index";
        }
        utilisateurService.creer(formulaire.username(), formulaire.motDePasse(), formulaire.role());
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.cree"));
        return "redirect:/parametres";
    }

    @PostMapping("/utilisateurs/{id}/actif")
    public String basculerActif(@PathVariable final UUID id, final RedirectAttributes redirectAttributes) {
        final var utilisateur = utilisateurService.trouver(id);
        utilisateurService.modifier(id, utilisateur.role(), !utilisateur.actif());
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.modifie"));
        return "redirect:/parametres";
    }

    private void enregistrerParametre(final String cle, final String valeur) {
        if (valeur != null && !valeur.isBlank()) {
            parametreService.enregistrer(cle, valeur);
        }
    }

    private void preparer(final Model model) {
        model.addAttribute("parametres", parametreService.toutesLesValeurs());
        model.addAttribute("utilisateurs", utilisateurService.lister());
        if (!model.containsAttribute("formulaire")) {
            model.addAttribute("formulaire", new UtilisateurFormulaire(null, null, null));
        }
        model.addAttribute("pageActive", "parametres");
    }
}
