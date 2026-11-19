package com.mfish.erp.simulateur;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Page du simulateur WhatsApp (outil de test de bout en bout).
 */
@Controller
@ConditionalOnProperty(name = "app.simulateur.active", havingValue = "true")
public class SimulateurWebController {

    @GetMapping("/simulateur")
    public String index() {
        return "simulateur/index";
    }

    /**
     * Expose le jeton CSRF au gabarit. Ce controleur est hors du perimetre couvert par
     * {@code WebControllerAdvice} (limite a {@code com.mfish.erp.web.controller}) : sans cet
     * attribut, la balise meta du gabarit reste vide et le POST du simulateur est refuse (403).
     */
    @ModelAttribute("csrf")
    public CsrfToken csrf(final HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = request.getAttribute("_csrf");
        }
        return token instanceof CsrfToken csrfToken ? csrfToken : null;
    }
}
