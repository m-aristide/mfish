package com.mfish.erp.web.controller;

import com.mfish.erp.web.FlashBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Page de connexion (authentification par formulaire geree par Spring Security).
 */
@Controller
public class ConnexionController {

    private final FlashBuilder flashBuilder;

    public ConnexionController(final FlashBuilder flashBuilder) {
        this.flashBuilder = flashBuilder;
    }

    @GetMapping("/connexion")
    public String connexion(@RequestParam(required = false) final String erreur,
                            @RequestParam(required = false) final String verrouille,
                            @RequestParam(required = false) final String deconnexion,
                            final Model model) {
        if (erreur != null) {
            model.addAttribute("flash", flashBuilder.erreur("connexion.erreur"));
        } else if (verrouille != null) {
            model.addAttribute("flash", flashBuilder.erreur("connexion.verrouille"));
        } else if (deconnexion != null) {
            model.addAttribute("flash", flashBuilder.info("connexion.deconnecte"));
        }
        return "connexion";
    }
}
