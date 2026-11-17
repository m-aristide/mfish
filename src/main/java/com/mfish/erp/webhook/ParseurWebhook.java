package com.mfish.erp.webhook;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Extrait les champs utiles d'un webhook Chatwoot depuis le JSON brut.
 */
@Component
public class ParseurWebhook {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public WebhookPayload parser(final String corps) {
        try {
            final JsonNode root = objectMapper.readTree(corps);
            return new WebhookPayload(
                    texte(root, "event"),
                    texte(root, "id"),
                    texte(root, "message_type"),
                    root.path("private").asBoolean(false),
                    texte(root, "content"),
                    texte(root.path("conversation"), "id"),
                    texte(root.path("sender"), "type"),
                    premierNonNull(texte(root.path("sender"), "phone_number"),
                            texte(root.path("contact"), "phone_number")),
                    premierNonNull(texte(root.path("sender"), "name"),
                            texte(root.path("contact"), "name")));
        } catch (JacksonException e) {
            throw new WebhookPayloadInvalideException("Payload Chatwoot malforme", e);
        }
    }

    private String texte(final JsonNode noeud, final String champ) {
        final JsonNode valeur = noeud.path(champ);
        if (valeur.isMissingNode() || valeur.isNull()) {
            return null;
        }
        return valeur.asText();
    }

    /**
     * Retourne la premiere valeur non nulle. Le payload webhook officiel de Chatwoot expose les
     * donnees du contact sous {@code sender} ({@code sender.phone_number}, {@code sender.name}),
     * tandis que les scripts locaux et les tests utilisent historiquement un objet {@code contact}.
     */
    private String premierNonNull(final String principal, final String repli) {
        return principal != null ? principal : repli;
    }
}
