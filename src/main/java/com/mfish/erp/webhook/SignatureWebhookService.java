package com.mfish.erp.webhook;

import com.mfish.erp.config.ChatwootProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Verification de la signature HMAC-SHA256 d'un webhook Chatwoot (secret partage).
 */
@Service
public class SignatureWebhookService {

    private final byte[] secret;

    public SignatureWebhookService(final ChatwootProperties properties) {
        if (properties.webhookSecret() == null || properties.webhookSecret().isBlank()) {
            throw new IllegalArgumentException("CHATWOOT_WEBHOOK_SECRET doit etre renseigne");
        }
        this.secret = properties.webhookSecret().getBytes(StandardCharsets.UTF_8);
    }

    public boolean estValide(final String corps, final String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        final byte[] attendu = hmac(corps).getBytes(StandardCharsets.UTF_8);
        final byte[] recu = signature.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(attendu, recu);
    }

    private String hmac(final String corps) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(corps.getBytes(StandardCharsets.UTF_8)));
        } catch (final Exception e) {
            throw new IllegalStateException("Impossible de calculer la signature du webhook", e);
        }
    }
}
