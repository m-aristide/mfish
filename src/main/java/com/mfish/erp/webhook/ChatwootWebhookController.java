package com.mfish.erp.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entree des webhooks Chatwoot. La signature est verifiee, le payload parse, puis le
 * traitement est declenche de facon asynchrone (reponse 200 rapide).
 */
@RestController
@RequestMapping("/webhooks")
public class ChatwootWebhookController {

    private static final String HEADER_SIGNATURE = "X-Chatwoot-Signature";
    private static final String HEADER_HORODATAGE = "X-Chatwoot-Timestamp";

    private final SignatureWebhookService signatureService;
    private final ParseurWebhook parseur;
    private final WebhookService webhookService;

    public ChatwootWebhookController(final SignatureWebhookService signatureService,
                                     final ParseurWebhook parseur,
                                     final WebhookService webhookService) {
        this.signatureService = signatureService;
        this.parseur = parseur;
        this.webhookService = webhookService;
    }

    @PostMapping("/chatwoot")
    public ResponseEntity<Void> recevoir(
            @RequestBody final String corps,
            @RequestHeader(value = HEADER_SIGNATURE, required = false) final String signature,
            @RequestHeader(value = HEADER_HORODATAGE, required = false) final String horodatage) {
        if (!signatureService.estValide(corps, signature, horodatage)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        final WebhookPayload payload;
        try {
            payload = parseur.parser(corps);
        } catch (WebhookPayloadInvalideException e) {
            return ResponseEntity.badRequest().build();
        }
        webhookService.traiter(payload);
        return ResponseEntity.ok().build();
    }
}
