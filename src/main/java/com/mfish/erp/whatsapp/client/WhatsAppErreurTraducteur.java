package com.mfish.erp.whatsapp.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * Traduit un code d'erreur Graph API (Meta) en exception metier typee.
 * Ne journalise jamais le jeton ni le corps de la reponse (uniquement le code).
 */
@Component
public class WhatsAppErreurTraducteur {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public WhatsAppException traduire(final RestClientResponseException e) {
        final String corps = e.getResponseBodyAsString();
        final int codeMeta = extraireCode(corps);
        if (codeMeta >= 0) {
            final String message = extraireMessage(corps, e.getStatusCode().value());
            return switch (codeMeta) {
                case 190 -> new WhatsAppTokenInvalideException(message);
                case 131030 -> new WhatsAppDestinataireInvalideException(message);
                case 131047 -> new WhatsAppHorsFenetreException(message);
                case 132000, 132001, 132012 -> new WhatsAppTemplateNonApprouveException(message);
                case 131026, 100, 80007 -> new WhatsAppLimiteDebitException(message);
                case 131049, 131053 -> new WhatsAppNumeroNonJoignableException(message);
                default -> new WhatsAppException(message);
            };
        }
        // Repli sur le statut HTTP quand le corps n'est pas exploitable (ex. 401 sans corps).
        final int statut = e.getStatusCode().value();
        return switch (statut) {
            case 401 -> new WhatsAppTokenInvalideException("Meta a repondu HTTP 401");
            case 429 -> new WhatsAppLimiteDebitException("Meta a repondu HTTP 429");
            default -> new WhatsAppException("Meta a repondu HTTP " + statut);
        };
    }

    private int extraireCode(final String corps) {
        try {
            final JsonNode root = jsonMapper.readTree(corps);
            return root.path("error").path("code").asInt(-1);
        } catch (Exception e) {
            return -1;
        }
    }

    private String extraireMessage(final String corps, final int statusHttp) {
        try {
            final JsonNode root = jsonMapper.readTree(corps);
            final String message = root.path("error").path("message").asText(null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (Exception e) {
            // le corps n'est pas du JSON exploitable
        }
        return "Meta a repondu HTTP " + statusHttp;
    }
}
