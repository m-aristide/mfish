package com.mfish.erp.whatsapp.webhook;

import com.mfish.erp.whatsapp.config.WhatsAppProperties;
import com.mfish.erp.whatsapp.metrics.WhatsAppMetrics;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entree des webhooks WhatsApp Cloud API (GET verification, POST reception).
 * Le POST repond 200 immediatement et traite en asynchrone ; la signature est verifiee
 * sur le corps brut avant toute deserialisation.
 */
@RestController
@RequestMapping("/webhooks/whatsapp")
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsAppWebhookController.class);
    private static final String HEADER_SIGNATURE = "X-Hub-Signature-256";

    private final WhatsAppSignatureService signatureService;
    private final WhatsAppPayloadParser parser;
    private final WhatsAppWebhookService webhookService;
    private final WhatsAppProperties properties;
    private final WhatsAppMetrics metrics;

    public WhatsAppWebhookController(final WhatsAppSignatureService signatureService,
                                     final WhatsAppPayloadParser parser,
                                     final WhatsAppWebhookService webhookService,
                                     final WhatsAppProperties properties,
                                     final WhatsAppMetrics metrics) {
        this.signatureService = signatureService;
        this.parser = parser;
        this.webhookService = webhookService;
        this.properties = properties;
        this.metrics = metrics;
    }

    @GetMapping
    public ResponseEntity<String> verifier(
            @RequestParam("hub.mode") final String mode,
            @RequestParam("hub.verify_token") final String token,
            @RequestParam("hub.challenge") final String challenge) {
        final byte[] recu = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        final byte[] attendu = properties.verifyToken() == null
                ? new byte[0]
                : properties.verifyToken().getBytes(StandardCharsets.UTF_8);
        if (!"subscribe".equals(mode) || !MessageDigest.isEqual(recu, attendu)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
    }

    @PostMapping
    public ResponseEntity<Void> recevoir(final HttpServletRequest request) {
        try {
            if (request.getContentLengthLong() > properties.maxBodySizeBytes()) {
                return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
            }
            final byte[] corps = request.getInputStream().readAllBytes();
            final String signature = request.getHeader(HEADER_SIGNATURE);
            if (!signatureService.estValide(corps, signature)) {
                metrics.incrementerSignaturesRejetees();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            webhookService.traiter(parser.parserMessages(corps), parser.parserStatuts(corps));
            return ResponseEntity.ok().build();
        } catch (WhatsAppPayloadInvalideException e) {
            LOG.warn("Payload WhatsApp malforme");
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            LOG.warn("Corps du webhook WhatsApp illisible");
            return ResponseEntity.badRequest().build();
        }
    }
}
