package com.mfish.erp.web.controller;

import com.mfish.erp.quartier.QuartierDto;
import com.mfish.erp.quartier.QuartierRequest;
import com.mfish.erp.quartier.QuartierService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.QuartierFormulaire;
import jakarta.validation.Valid;
import java.util.Comparator;
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
 * Ecran « Quartiers » : referentiel des zones de livraison (liste, creation,
 * modification, activation/desactivation). Les quartiers desactives restent
 * consultables pour l'historique des commandes mais ne devraient plus etre
 * retenus pour de nouvelles livraisons.
 */
@Controller
@RequestMapping("/quartiers")
public class QuartierWebController {

    /** Ordre d'affichage : ordre de tournee par defaut, puis nom alphabetique. */
    private static final Comparator<QuartierDto> ORDRE_AFFICHAGE =
            Comparator.comparingInt(QuartierDto::ordreDefaut).thenComparing(QuartierDto::nom);

    private final QuartierService quartierService;
    private final FlashBuilder flashBuilder;

    public QuartierWebController(final QuartierService quartierService,
                                 final FlashBuilder flashBuilder) {
        this.quartierService = quartierService;
        this.flashBuilder = flashBuilder;
    }

    @GetMapping
    public String lister(final Model model) {
        model.addAttribute("quartiers", quartierService.lister().stream()
                .sorted(ORDRE_AFFICHAGE)
                .toList());
        model.addAttribute("pageActive", "quartiers");
        return "quartiers/liste";
    }

    @GetMapping("/nouveau")
    public String formulaireCreation(final Model model) {
        preparerFormulaire(model, new QuartierFormulaire(null, null, null, null, 0, true));
        return "quartiers/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("formulaire") final QuartierFormulaire formulaire,
                        final BindingResult bindingResult, final Model model,
                        final RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            nomDejaUtilise(formulaire.nom(), null, bindingResult);
        }
        if (bindingResult.hasErrors()) {
            return formulaireInvalide(model, formulaire, null);
        }
        quartierService.creer(requete(formulaire));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.cree"));
        return "redirect:/quartiers";
    }

    @GetMapping("/{id}/modifier")
    public String formulaireModification(@PathVariable final UUID id, final Model model) {
        final QuartierDto quartier = quartierService.trouver(id);
        preparerFormulaire(model, new QuartierFormulaire(quartier.nom(), quartier.zone(),
                quartier.latitude(), quartier.longitude(), quartier.ordreDefaut(),
                quartier.actif()));
        model.addAttribute("quartierId", id);
        return "quartiers/formulaire";
    }

    @PostMapping("/{id}")
    public String modifier(@PathVariable final UUID id,
                           @Valid @ModelAttribute("formulaire") final QuartierFormulaire formulaire,
                           final BindingResult bindingResult, final Model model,
                           final RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            nomDejaUtilise(formulaire.nom(), id, bindingResult);
        }
        if (bindingResult.hasErrors()) {
            return formulaireInvalide(model, formulaire, id);
        }
        quartierService.modifier(id, requete(formulaire));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.modifie"));
        return "redirect:/quartiers";
    }

    @PostMapping("/{id}/basculer")
    public String basculer(@PathVariable final UUID id,
                           final RedirectAttributes redirectAttributes) {
        final QuartierDto quartier = quartierService.trouver(id);
        quartierService.modifier(id, new QuartierRequest(quartier.nom(), quartier.zone(),
                quartier.latitude(), quartier.longitude(), quartier.ordreDefaut(),
                !quartier.actif()));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.modifie"));
        return "redirect:/quartiers";
    }

    /**
     * Rejette le nom s'il est deja porte par un autre quartier (casse et espaces
     * de saisie ignores) pour eviter une violation de contrainte d'unicite.
     */
    private void nomDejaUtilise(final String nom, final UUID idExclu,
                                final BindingResult bindingResult) {
        final String nomNettoye = texteNettoye(nom);
        if (nomNettoye != null && quartierService.nomDejaUtilise(nomNettoye, idExclu)) {
            bindingResult.rejectValue("nom", "validation.quartierExiste");
        }
    }

    private String formulaireInvalide(final Model model, final QuartierFormulaire formulaire,
                                      final UUID quartierId) {
        preparerFormulaire(model, formulaire);
        if (quartierId != null) {
            model.addAttribute("quartierId", quartierId);
        }
        return "quartiers/formulaire";
    }

    private static QuartierRequest requete(final QuartierFormulaire formulaire) {
        return new QuartierRequest(texteNettoye(formulaire.nom()), texteNettoye(formulaire.zone()),
                formulaire.latitude(), formulaire.longitude(), formulaire.ordreDefaut(),
                Boolean.TRUE.equals(formulaire.actif()));
    }

    /**
     * Normalise une saisie libre : espaces superflus retires, chaine vide convertie
     * en {@code null} pour ne pas stocker de valeur vide en base.
     */
    private static String texteNettoye(final String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }

    private void preparerFormulaire(final Model model, final QuartierFormulaire formulaire) {
        model.addAttribute("formulaire", formulaire);
        model.addAttribute("pageActive", "quartiers");
    }
}
