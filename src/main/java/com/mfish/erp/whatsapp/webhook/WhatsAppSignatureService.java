package com.mfish.erp.whatsapp.webhook;

import com.mfish.erp.whatsapp.config.WhatsAppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Verification de la signature {@code X-Hub-Signature-256} (HMAC-SHA256 du corps brut,
 * avec l'App Secret) en temps constant via {@link MessageDigest#isEqual}.
 */
@Service
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppSignatureService {

    private static final String PREFIXE = "sha256=";

    private final byte[] secret;

    public WhatsAppSignatureService(final WhatsAppProperties properties) {
        this.secret = properties.appSecret().getBytes(StandardCharsets.UTF_8);
    }

    public boolean estValide(final byte[] corps, final String signature) {
        if (signature == null || signature.isBlank() || !signature.startsWith(PREFIXE)) {
            return false;
        }
        final byte[] recu;
        try {
            recu = HexFormat.of().parseHex(signature.substring(PREFIXE.length()).trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(hmac(corps), recu);
    }

    private byte[] hmac(final byte[] corps) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(corps);
        } catch (final Exception e) {
            throw new IllegalStateException("Impossible de calculer la signature du webhook WhatsApp", e);
        }
    }
}
