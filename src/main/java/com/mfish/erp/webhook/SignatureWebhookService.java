package com.mfish.erp.webhook;

import com.mfish.erp.config.ChatwootProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Verification de la signature HMAC-SHA256 d'un webhook Chatwoot (secret partage).
 *
 * <p>Chatwoot signe ses webhooks au format
 * {@code sha256=<HMAC-SHA256(secret, "<timestamp>.<corps brut>")>} et transmet l'horodatage
 * dans l'en-tete {@code X-Chatwoot-Timestamp}. Le format historique (HMAC hexadecimal du corps
 * seul, sans prefixe ni horodatage) reste accepte pour les scripts locaux et les tests.</p>
 */
@Service
public class SignatureWebhookService {

    private static final String PREFIXE = "sha256=";

    /** Tolerance appliquee a l'horodatage Chatwoot (protection contre le rejeu). */
    private static final Duration TOLERANCE_HORODATAGE = Duration.ofMinutes(5);

    private final byte[] secret;
    private final Clock clock;

    public SignatureWebhookService(final ChatwootProperties properties, final Clock clock) {
        if (properties.webhookSecret() == null || properties.webhookSecret().isBlank()) {
            throw new IllegalArgumentException("CHATWOOT_WEBHOOK_SECRET doit etre renseigne");
        }
        this.secret = properties.webhookSecret().getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    /**
     * Verifie la signature d'un webhook Chatwoot.
     *
     * @param corps      corps brut de la requete (jamais re-serialise)
     * @param signature  valeur de l'en-tete {@code X-Chatwoot-Signature}
     * @param horodatage valeur de l'en-tete {@code X-Chatwoot-Timestamp} (facultatif)
     * @return {@code true} si la signature correspond au corps
     */
    public boolean estValide(final String corps, final String signature,
                             final String horodatage) {
        if (corps == null || signature == null || signature.isBlank()) {
            return false;
        }
        final String recue = signature.trim();
        if (horodatage == null || horodatage.isBlank()) {
            return egal(hmac(corps), recue);
        }
        return estValideAvecHorodatage(corps, recue, horodatage.trim());
    }

    /**
     * Variante sans horodatage : format historique (HMAC hexadecimal du corps seul).
     */
    public boolean estValide(final String corps, final String signature) {
        return estValide(corps, signature, null);
    }

    private boolean estValideAvecHorodatage(final String corps, final String signature,
                                            final String horodatage) {
        final long seconde;
        try {
            seconde = Long.parseLong(horodatage);
        } catch (NumberFormatException e) {
            return false;
        }
        final long ecart = Math.abs(clock.instant().getEpochSecond() - seconde);
        if (ecart > TOLERANCE_HORODATAGE.toSeconds()) {
            return false;
        }
        return egal(PREFIXE + hmac(horodatage + "." + corps), signature);
    }

    private static boolean egal(final String attendu, final String recu) {
        return MessageDigest.isEqual(attendu.getBytes(StandardCharsets.UTF_8),
                recu.getBytes(StandardCharsets.UTF_8));
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
