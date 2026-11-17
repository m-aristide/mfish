package com.mfish.erp.web.controller;

import com.mfish.erp.produit.ProduitRequest;
import com.mfish.erp.produit.ProduitService;
import com.mfish.erp.stockage.StockageException;
import com.mfish.erp.stockage.StockageService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.ProduitFormulaire;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ecran « Produits / catalogue » : liste, creation, modification, activation/desactivation.
 */
@Controller
@RequestMapping("/produits")
public class ProduitWebController {

    private final ProduitService produitService;
    private final StockageService stockageService;
    private final FlashBuilder flashBuilder;

    public ProduitWebController(final ProduitService produitService,
                                final StockageService stockageService,
                                final FlashBuilder flashBuilder) {
        this.produitService = produitService;
        this.stockageService = stockageService;
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
        preparerFormulaire(model, new ProduitFormulaire(null, 0, null, true, null));
        return "produits/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("formulaire") final ProduitFormulaire formulaire,
                        final BindingResult bindingResult, final HttpServletRequest request,
                        final Model model, final RedirectAttributes redirectAttributes) {
        final String image = resoudreImage(extraireImage(request), bindingResult, null);
        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, formulaire);
            return "produits/formulaire";
        }
        produitService.creer(requete(formulaire, image));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.cree"));
        return "redirect:/produits";
    }

    @GetMapping("/{id}/modifier")
    public String formulaireModification(@PathVariable final UUID id, final Model model) {
        final var produit = produitService.trouver(id);
        preparerFormulaire(model, new ProduitFormulaire(produit.nom(), produit.prix(),
                produit.unite(), produit.actif(), produit.image()));
        model.addAttribute("produitId", id);
        return "produits/formulaire";
    }

    @PostMapping("/{id}")
    public String modifier(@PathVariable final UUID id,
                           @Valid @ModelAttribute("formulaire") final ProduitFormulaire formulaire,
                           final BindingResult bindingResult, final HttpServletRequest request,
                           final Model model, final RedirectAttributes redirectAttributes) {
        final String imageExistante = produitService.trouver(id).image();
        final String image = resoudreImage(extraireImage(request), bindingResult, imageExistante);
        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, formulaire);
            model.addAttribute("produitId", id);
            return "produits/formulaire";
        }
        produitService.modifier(id, requete(formulaire, image));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.modifie"));
        return "redirect:/produits";
    }

    @PostMapping("/{id}/basculer")
    public String basculer(@PathVariable final UUID id, final RedirectAttributes redirectAttributes) {
        final var produit = produitService.trouver(id);
        produitService.modifier(id, new ProduitRequest(produit.nom(), produit.prix(),
                produit.unite(), !produit.actif(), produit.image()));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.modifie"));
        return "redirect:/produits";
    }

    private ProduitRequest requete(final ProduitFormulaire formulaire, final String image) {
        return new ProduitRequest(formulaire.nom(), formulaire.prix(), formulaire.unite(),
                Boolean.TRUE.equals(formulaire.actif()), image);
    }

    /**
     * Resout le chemin final de l'image : stocke le fichier televerse (en supprimant
     * l'ancien le cas echeant) ou conserve l'image existante. Rejette le champ
     * {@code imageFichier} si aucune image n'est fournie.
     */
    private String resoudreImage(final MultipartFile imageFichier, final BindingResult bindingResult,
                                 final String imageExistante) {
        if (imageFichier != null && !imageFichier.isEmpty()) {
            try {
                final String chemin = stockageService.stocker(imageFichier, "produits");
                if (imageExistante != null && !imageExistante.isBlank()) {
                    stockageService.supprimer(imageExistante);
                }
                return chemin;
            } catch (StockageException exception) {
                bindingResult.rejectValue("imageExistante", "validation.imageType");
                return null;
            }
        }
        if (imageExistante != null && !imageExistante.isBlank()) {
            return imageExistante;
        }
        bindingResult.rejectValue("imageExistante", "validation.image");
        return null;
    }

    private static MultipartFile extraireImage(final HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest multipart) {
            return multipart.getFile("imageFichier");
        }
        return null;
    }

    private void preparerFormulaire(final Model model, final ProduitFormulaire formulaire) {
        model.addAttribute("formulaire", formulaire);
        model.addAttribute("pageActive", "produits");
    }
}
