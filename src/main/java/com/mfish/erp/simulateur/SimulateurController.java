package com.mfish.erp.simulateur;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de l'interface du simulateur : envoi d'un message et lecture des reponses (IA) par
 * polling. Selon le mode, le message est rejoue en webhook local ou envoye a Chatwoot.
 */
@RestController
@RequestMapping("/simulateur/api")
@ConditionalOnProperty(name = "app.simulateur.active", havingValue = "true")
public class SimulateurController {

    private final SimulateurService simulateurService;

    public SimulateurController(final SimulateurService simulateurService) {
        this.simulateurService = simulateurService;
    }

    @PostMapping("/messages")
    public ResponseEntity<SimulateurMessageRequest> envoyer(
            @RequestBody final SimulateurMessageRequest requete) {
        if (!estValide(requete)) {
            return ResponseEntity.badRequest().build();
        }
        simulateurService.envoyer(requete);
        return ResponseEntity.ok(requete);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<SimulateurMessage> messages(@PathVariable final String id) {
        return simulateurService.listerMessages(id);
    }

    @DeleteMapping("/conversations/{id}/messages")
    public ResponseEntity<Void> effacer(@PathVariable final String id) {
        simulateurService.effacer(id);
        return ResponseEntity.noContent().build();
    }

    private static boolean estValide(final SimulateurMessageRequest requete) {
        return requete != null
                && nonVide(requete.conversationId())
                && nonVide(requete.telephone())
                && nonVide(requete.contenu());
    }

    private static boolean nonVide(final String valeur) {
        return valeur != null && !valeur.isBlank();
    }
}
