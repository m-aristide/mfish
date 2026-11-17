package com.mfish.erp.whatsapp.config;

import com.mfish.erp.config.RetryProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration de l'integration officielle WhatsApp Cloud API (Meta).
 *
 * <p>Les secrets sont injectes par variables d'environnement ({@code WHATSAPP_*}).
 * Lorsque {@code enabled} vaut {@code false} (ou que la configuration est absente),
 * le module demarre en mode desactive explicite : aucun bean d'appel Meta n'est cree,
 * l'application reste fonctionnelle.
 *
 * @param accessToken        jeton d'acces Graph API (jamais journalise)
 * @param phoneNumberId      identifiant du numero WhatsApp professionnel
 * @param businessAccountId  identifiant du WABA (WhatsApp Business Account)
 * @param appSecret          secret de l'application Meta (verification de signature)
 * @param verifyToken        jeton de verification du webhook (choisi par l'operateur)
 * @param apiVersion         version de l'API Graph (rendue configurable, ex. v21.0)
 * @param baseUrl            URL de base de Graph API (https://graph.facebook.com)
 * @param testRecipient      numero de test unique autorise pendant la mission
 * @param maxBodySizeBytes   taille maximale du corps de webhook accepte
 */
@Validated
@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(
        boolean enabled,
        String accessToken,
        String phoneNumberId,
        String businessAccountId,
        String appSecret,
        String verifyToken,
        String apiVersion,
        String baseUrl,
        String testRecipient,
        int maxBodySizeBytes,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Valid @NotNull RetryProperties retry) {

    @AssertTrue(message = "whatsapp.enabled=true exige accessToken, phoneNumberId, appSecret et verifyToken")
    public boolean isConfigurationComplete() {
        if (!enabled) {
            return true;
        }
        return nonVide(accessToken)
                && nonVide(phoneNumberId)
                && nonVide(appSecret)
                && nonVide(verifyToken);
    }

    private static boolean nonVide(final String valeur) {
        return valeur != null && !valeur.isBlank();
    }
}
