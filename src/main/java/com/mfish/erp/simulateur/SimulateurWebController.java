package com.mfish.erp.simulateur;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
}
