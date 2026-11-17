package com.mfish.erp.whatsapp.webhook;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Extrait les messages entrants et les notifications de statut d'un webhook WhatsApp
 * (structure {@code entry[].changes[].value}).
 */
@Component
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppPayloadParser {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public List<MessageEntrant> parserMessages(final byte[] corps) {
        final List<MessageEntrant> resultat = new ArrayList<>();
        final JsonNode racine = lire(corps);
        for (final JsonNode valeur : valeurs(racine)) {
            final String nomContact = texte(valeur.path("contacts").path(0).path("profile"), "name");
            for (final JsonNode message : valeur.path("messages")) {
                final String wamid = texte(message, "id");
                final String telephone = texte(message, "from");
                final String typeMeta = texte(message, "type");
                final TypeMessage type = typeMessage(typeMeta);
                resultat.add(new MessageEntrant(
                        wamid,
                        telephone,
                        nomContact,
                        type,
                        extraireTexte(message, typeMeta),
                        extraireMediaId(message, typeMeta)));
            }
        }
        return resultat;
    }

    public List<StatutEntrant> parserStatuts(final byte[] corps) {
        final List<StatutEntrant> resultat = new ArrayList<>();
        final JsonNode racine = lire(corps);
        for (final JsonNode valeur : valeurs(racine)) {
            for (final JsonNode statut : valeur.path("statuses")) {
                resultat.add(new StatutEntrant(
                        texte(statut, "id"),
                        texte(statut, "status"),
                        codeErreur(statut)));
            }
        }
        return resultat;
    }

    private JsonNode lire(final byte[] corps) {
        try {
            return jsonMapper.readTree(corps);
        } catch (Exception e) {
            throw new WhatsAppPayloadInvalideException("Payload WhatsApp malforme", e);
        }
    }

    private List<JsonNode> valeurs(final JsonNode racine) {
        final List<JsonNode> resultat = new ArrayList<>();
        for (final JsonNode entree : racine.path("entry")) {
            for (final JsonNode changement : entree.path("changes")) {
                resultat.add(changement.path("value"));
            }
        }
        return resultat;
    }

    private TypeMessage typeMessage(final String typeMeta) {
        if (typeMeta == null) {
            return TypeMessage.INCONNU;
        }
        return switch (typeMeta) {
            case "text" -> TypeMessage.TEXTE;
            case "image" -> TypeMessage.IMAGE;
            case "audio", "voice" -> TypeMessage.AUDIO;
            case "document" -> TypeMessage.DOCUMENT;
            case "location" -> TypeMessage.LOCALISATION;
            case "contacts" -> TypeMessage.CONTACT;
            case "button" -> TypeMessage.BOUTON;
            case "interactive" -> TypeMessage.REPONSE_INTERACTIVE;
            case "reaction" -> TypeMessage.REACTION;
            case "unsupported" -> TypeMessage.NON_SUPPORTE;
            default -> TypeMessage.INCONNU;
        };
    }

    private String extraireTexte(final JsonNode message, final String typeMeta) {
        if (typeMeta == null) {
            return null;
        }
        return switch (typeMeta) {
            case "text" -> texte(message.path("text"), "body");
            case "button" -> texte(message.path("button"), "text");
            case "interactive" -> texteInteractif(message.path("interactive"));
            case "reaction" -> texte(message.path("reaction"), "emoji");
            default -> null;
        };
    }

    private String texteInteractif(final JsonNode interactive) {
        final String type = texte(interactive, "type");
        if ("button_reply".equals(type)) {
            return texte(interactive.path("button_reply"), "title");
        }
        if ("list_reply".equals(type)) {
            return texte(interactive.path("list_reply"), "title");
        }
        return null;
    }

    private String extraireMediaId(final JsonNode message, final String typeMeta) {
        if (typeMeta == null) {
            return null;
        }
        return switch (typeMeta) {
            case "image" -> texte(message.path("image"), "id");
            case "audio", "voice" -> texte(message.path("audio"), "id");
            case "document" -> texte(message.path("document"), "id");
            case "video" -> texte(message.path("video"), "id");
            default -> null;
        };
    }

    private String codeErreur(final JsonNode statut) {
        final JsonNode erreurs = statut.path("errors");
        if (!erreurs.isArray() || erreurs.isEmpty()) {
            return null;
        }
        return texte(erreurs.get(0), "code");
    }

    private String texte(final JsonNode noeud, final String champ) {
        final JsonNode valeur = noeud.path(champ);
        if (valeur.isMissingNode() || valeur.isNull()) {
            return null;
        }
        return valeur.asText();
    }
}
