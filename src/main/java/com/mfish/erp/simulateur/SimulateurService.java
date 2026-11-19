package com.mfish.erp.simulateur;

import com.mfish.erp.config.ChatwootProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cœur du simulateur. Deux modes :
 * <ul>
 *   <li>{@code LOCAL} : rejoue un webhook signe vers {@code /webhooks/chatwoot} et capture les
 *       reponses de l'IA en memoire (sans Chatwoot) ;</li>
 *   <li>{@code CHATWOOT} : cree le contact et la conversation dans Chatwoot (API publique),
 *       Chatwoot declenche le webhook, l'IA repond via {@code ChatwootClient} ; la conversation
 *       est alors visible dans Chatwoot (telephone compris).</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "app.simulateur.active", havingValue = "true")
public class SimulateurService {

    private static final Logger LOG = LoggerFactory.getLogger(SimulateurService.class);
    private static final String MODE_CHATWOOT = "CHATWOOT";

    private final ChatwootProperties chatwootProperties;
    private final Clock clock;
    private final SimulateurMessageStore messageStore;
    private final SimulateurConversationStore conversationStore;
    private final String mode;
    private final String inbox;
    private final RestClient webhookClient;
    private final RestClient publicClient;
    private final RestClient authClient;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public SimulateurService(final ChatwootProperties chatwootProperties,
                             final Clock clock,
                             final SimulateurMessageStore messageStore,
                             final SimulateurConversationStore conversationStore,
                             @Value("${app.simulateur.mode:LOCAL}") final String mode,
                             @Value("${app.simulateur.inbox:}") final String inbox,
                             @Value("${server.port:8080}") final int port) {
        this.chatwootProperties = chatwootProperties;
        this.clock = clock;
        this.messageStore = messageStore;
        this.conversationStore = conversationStore;
        this.mode = mode;
        this.inbox = inbox;
        this.webhookClient = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        if (chatwootProperties.baseUrl() == null || chatwootProperties.baseUrl().isBlank()) {
            this.publicClient = null;
            this.authClient = null;
        } else {
            this.publicClient = RestClient.builder().baseUrl(chatwootProperties.baseUrl()).build();
            this.authClient = RestClient.builder().baseUrl(chatwootProperties.baseUrl())
                    .defaultHeader("api_access_token", chatwootProperties.apiToken()).build();
        }
    }

    public void envoyer(final SimulateurMessageRequest requete) {
        if (MODE_CHATWOOT.equalsIgnoreCase(mode)) {
            envoyerChatwoot(requete);
        } else {
            envoyerLocal(requete);
        }
    }

    public List<SimulateurMessage> listerMessages(final String conversationId) {
        if (MODE_CHATWOOT.equalsIgnoreCase(mode)) {
            return lireChatwoot(conversationId);
        }
        return messageStore.messages(conversationId);
    }

    public void effacer(final String conversationId) {
        messageStore.effacer(conversationId);
        conversationStore.effacer(conversationId);
    }

    // ---- mode LOCAL ---------------------------------------------------------
    private void envoyerLocal(final SimulateurMessageRequest requete) {
        final String corps = corpsWebhook(requete);
        final long horodatage = clock.instant().getEpochSecond();
        final String signature = "sha256=" + hmac(horodatage + "." + corps);

        LOG.info("Simulateur : envoi d'un message vers /webhooks/chatwoot (conversation {})",
                requete.conversationId());
        webhookClient.post()
                .uri("/webhooks/chatwoot")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Chatwoot-Signature", signature)
                .header("X-Chatwoot-Timestamp", String.valueOf(horodatage))
                .body(corps)
                .retrieve()
                .toBodilessEntity();
    }

    // ---- mode CHATWOOT ------------------------------------------------------
    private void envoyerChatwoot(final SimulateurMessageRequest requete) {
        verifierConfigurationChatwoot();
        SimulateurConversation reference = conversationStore.trouver(requete.conversationId()).orElse(null);
        if (reference == null) {
            final String sourceId = creerContact(requete);
            final String chatwootConversationId = creerConversation(sourceId);
            reference = new SimulateurConversation(sourceId, chatwootConversationId);
            conversationStore.enregistrer(requete.conversationId(), reference);
        }
        envoyerMessageChatwoot(reference, requete.contenu());
        LOG.info("Simulateur : message envoye a Chatwoot (conversation {})",
                reference.chatwootConversationId());
    }

    private String creerContact(final SimulateurMessageRequest requete) {
        final Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("identifier", "sim-" + requete.conversationId());
        corps.put("name", requete.nom());
        corps.put("phone_number", requete.telephone());
        final String reponse = publicClient.post()
                .uri("/public/api/v1/inboxes/{inbox}/contacts", inbox)
                .contentType(MediaType.APPLICATION_JSON)
                .body(serialiser(corps))
                .retrieve()
                .body(String.class);
        return lireChamp(reponse, "source_id");
    }

    private String creerConversation(final String sourceId) {
        final String reponse = publicClient.post()
                .uri("/public/api/v1/inboxes/{inbox}/contacts/{sourceId}/conversations", inbox, sourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .retrieve()
                .body(String.class);
        return lireChamp(reponse, "id");
    }

    private void envoyerMessageChatwoot(final SimulateurConversation reference, final String contenu) {
        publicClient.post()
                .uri("/public/api/v1/inboxes/{inbox}/contacts/{sourceId}/conversations/{conversationId}/messages",
                        inbox, reference.sourceId(), reference.chatwootConversationId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(serialiser(Map.of("content", contenu)))
                .retrieve()
                .toBodilessEntity();
    }

    private List<SimulateurMessage> lireChatwoot(final String conversationId) {
        verifierConfigurationChatwoot();
        final SimulateurConversation reference = conversationStore.trouver(conversationId).orElse(null);
        if (reference == null) {
            return List.of();
        }
        final String reponse = authClient.get()
                .uri("/api/v1/accounts/{accountId}/conversations/{conversationId}/messages",
                        chatwootProperties.accountId(), reference.chatwootConversationId())
                .retrieve()
                .body(String.class);
        final List<SimulateurMessage> messages = new ArrayList<>();
        try {
            final JsonNode racine = objectMapper.readTree(reponse);
            final JsonNode payload = racine.path("payload");
            if (payload.isArray()) {
                for (final JsonNode message : payload) {
                    if (!estSortant(message.path("message_type"))) {
                        continue;
                    }
                    final long created = message.path("created_at").asLong(0);
                    messages.add(new SimulateurMessage(conversationId,
                            message.path("content").asText(),
                            created > 0 ? Instant.ofEpochSecond(created) : clock.instant()));
                }
            }
        } catch (JacksonException e) {
            LOG.warn("Simulateur : impossible de lire les messages Chatwoot", e);
        }
        return messages;
    }

    /**
     * Chatwoot renvoie {@code message_type} sous forme d'entier dans l'API REST
     * (0 = entrant, 1 = sortant, 2 = activite) et de chaine dans les webhooks :
     * les deux formes sont acceptees, seule une reponse sortante est retenue.
     */
    private boolean estSortant(final JsonNode type) {
        if (type.isNumber()) {
            return type.asInt() == 1;
        }
        final String valeur = type.asText();
        return "outgoing".equalsIgnoreCase(valeur) || "1".equals(valeur);
    }

    private void verifierConfigurationChatwoot() {
        if (publicClient == null || authClient == null
                || inbox == null || inbox.isBlank()
                || chatwootProperties.apiToken() == null || chatwootProperties.apiToken().isBlank()
                || chatwootProperties.accountId() == null || chatwootProperties.accountId().isBlank()) {
            throw new IllegalStateException(
                    "Simulateur mode CHATWOOT : renseigner CHATWOOT_BASE_URL, CHATWOOT_API_TOKEN, "
                            + "CHATWOOT_ACCOUNT_ID et APP_SIMULATEUR_INBOX");
        }
    }

    // ---- helpers ------------------------------------------------------------
    private String serialiser(final Object corps) {
        try {
            return objectMapper.writeValueAsString(corps);
        } catch (JacksonException e) {
            throw new IllegalStateException("Impossible de serialiser le payload du simulateur", e);
        }
    }

    private String lireChamp(final String json, final String champ) {
        try {
            return objectMapper.readTree(json).path(champ).asText();
        } catch (JacksonException e) {
            throw new IllegalStateException("Reponse Chatwoot inattendue", e);
        }
    }

    private String corpsWebhook(final SimulateurMessageRequest requete) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "message_created");
        payload.put("id", "sim-" + UUID.randomUUID());
        payload.put("message_type", "incoming");
        payload.put("private", false);
        payload.put("content", requete.contenu());
        payload.put("conversation", Map.of("id", requete.conversationId()));

        final Map<String, Object> sender = new LinkedHashMap<>();
        sender.put("type", "contact");
        sender.put("phone_number", requete.telephone());
        sender.put("name", requete.nom());
        payload.put("sender", sender);

        return serialiser(payload);
    }

    private String hmac(final String corps) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    chatwootProperties.webhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(corps.getBytes(StandardCharsets.UTF_8)));
        } catch (final Exception e) {
            throw new IllegalStateException("Impossible de signer le payload du simulateur", e);
        }
    }
}
