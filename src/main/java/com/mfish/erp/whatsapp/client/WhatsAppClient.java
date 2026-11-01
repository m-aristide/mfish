package com.mfish.erp.whatsapp.client;

import com.mfish.erp.config.RetryProperties;
import com.mfish.erp.whatsapp.Masquage;
import com.mfish.erp.whatsapp.config.WhatsAppProperties;
import com.mfish.erp.whatsapp.metrics.WhatsAppMetrics;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptateur sortant WhatsApp Cloud API (Graph API). Timeout, retry sur erreurs
 * transitoires uniquement (5xx, timeout, 429 en respectant Retry-After), traduction
 * des codes d'erreur Meta en exceptions typees. Jamais de jeton ni de contenu en log.
 */
@Component
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppClient implements WhatsAppPort {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsAppClient.class);
    private static final int LIMITE_CARACTERES = 4096;
    private static final int MAX_MEDIA_OCTETS = 10 * 1024 * 1024;

    private final RestClient restClient;
    private final WhatsAppProperties properties;
    private final WhatsAppErreurTraducteur traducteur;
    private final WhatsAppMetrics metrics;

    public WhatsAppClient(final RestClient whatsappRestClient,
                          final WhatsAppProperties properties,
                          final WhatsAppErreurTraducteur traducteur,
                          final WhatsAppMetrics metrics) {
        this.restClient = whatsappRestClient;
        this.properties = properties;
        this.traducteur = traducteur;
        this.metrics = metrics;
    }

    @Override
    public String envoyerTexte(final String telephone, final String contenu) {
        String dernier = null;
        for (final String morceau : DecoupeurMessage.decouper(contenu, LIMITE_CARACTERES)) {
            dernier = poster(requeteTexte(telephone, morceau));
        }
        return dernier;
    }

    @Override
    public String envoyerTemplate(final String telephone, final String nom, final String langue,
                                  final List<String> variablesCorps) {
        return poster(requeteTemplate(telephone, nom, langue, variablesCorps));
    }

    @Override
    public String envoyerReponseInteractive(final String telephone, final String corps,
                                            final List<String> options) {
        return poster(requeteInteractive(telephone, corps, options));
    }

    @Override
    public void marquerLu(final String telephone, final String wamid) {
        executerAvecRetry(() -> restClient.post()
                .uri("/{version}/{phoneNumberId}/messages",
                        properties.apiVersion(), properties.phoneNumberId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .body(Map.of("messaging_product", "whatsapp", "status", "read", "message_id", wamid))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void indicateurSaisie(final String telephone) {
        // La Cloud API ne documente pas l'indicateur de saisie : tentative best-effort,
        // echec silencieux (aucun impact sur le flux).
        try {
            executerAvecRetry(() -> restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages",
                            properties.apiVersion(), properties.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                    .body(Map.of("messaging_product", "whatsapp", "to", telephone, "status", "typing"))
                    .retrieve()
                    .toBodilessEntity());
        } catch (WhatsAppException e) {
            LOG.debug("Indicateur de saisie non supporte pour {}", Masquage.telephone(telephone));
        }
    }

    @Override
    public MediaTelecharge telechargerMedia(final String mediaId) {
        final MediaMeta meta = executerAvecRetry(() -> restClient.get()
                .uri("/{version}/{mediaId}", properties.apiVersion(), mediaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .retrieve()
                .body(MediaMeta.class));
        if (meta == null || meta.url() == null || meta.url().isBlank()) {
            throw new WhatsAppException("URL de media introuvable pour l'identifiant fourni");
        }
        final byte[] contenu = executerAvecRetry(() -> restClient.get()
                .uri(meta.url())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .retrieve()
                .body(byte[].class));
        validerMedia(contenu, meta.mimeType());
        return new MediaTelecharge(contenu, meta.mimeType());
    }

    private void validerMedia(final byte[] contenu, final String typeMime) {
        if (contenu == null || contenu.length == 0) {
            throw new WhatsAppException("Media vide");
        }
        if (contenu.length > MAX_MEDIA_OCTETS) {
            throw new WhatsAppException("Media trop volumineux (limite 10 Mo)");
        }
        if (typeMime == null || !typeMime.startsWith("image/")
                && !typeMime.startsWith("audio/")
                && !typeMime.startsWith("video/")
                && !typeMime.equals("application/pdf")
                && !typeMime.equals("text/plain")) {
            throw new WhatsAppException("Type MIME non supporte : " + typeMime);
        }
    }

    private String poster(final Map<String, Object> corps) {
        final ReponseEnvoi reponse = metrics.mesurerMeta(() -> executerAvecRetry(() -> restClient.post()
                .uri("/{version}/{phoneNumberId}/messages",
                        properties.apiVersion(), properties.phoneNumberId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .body(corps)
                .retrieve()
                .body(ReponseEnvoi.class)));
        if (reponse == null || reponse.messages() == null || reponse.messages().isEmpty()) {
            throw new WhatsAppException("Reponse d'envoi WhatsApp invalide");
        }
        final String wamid = reponse.messages().get(0).id();
        metrics.incrementerSortants();
        LOG.info("Message WhatsApp envoye, wamid={}", wamid);
        return wamid;
    }

    private Map<String, Object> requeteTexte(final String telephone, final String contenu) {
        final Map<String, Object> corps = baseCorps(telephone);
        corps.put("type", "text");
        corps.put("text", Map.of("body", contenu));
        return corps;
    }

    private Map<String, Object> requeteTemplate(final String telephone, final String nom,
                                                final String langue,
                                                final List<String> variablesCorps) {
        final Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", nom);
        template.put("language", Map.of("code", langue));
        if (variablesCorps != null && !variablesCorps.isEmpty()) {
            final List<Map<String, String>> parametres = variablesCorps.stream()
                    .map(v -> Map.of("type", "text", "text", v))
                    .toList();
            template.put("components", List.of(Map.of("type", "body", "parameters", parametres)));
        }
        final Map<String, Object> corps = baseCorps(telephone);
        corps.put("type", "template");
        corps.put("template", template);
        return corps;
    }

    private Map<String, Object> requeteInteractive(final String telephone, final String texte,
                                                   final List<String> options) {
        final List<String> boutons = options == null ? List.of() : options.stream().limit(3).toList();
        final List<Map<String, Object>> boutonsReponse = new ArrayList<>();
        for (int i = 0; i < boutons.size(); i++) {
            boutonsReponse.add(Map.of("type", "reply",
                    "reply", Map.of("id", "opt_" + (i + 1), "title", boutons.get(i))));
        }
        final Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "button");
        interactive.put("body", Map.of("text", texte));
        interactive.put("action", Map.of("buttons", boutonsReponse));
        final Map<String, Object> corps = baseCorps(telephone);
        corps.put("type", "interactive");
        corps.put("interactive", interactive);
        return corps;
    }

    private Map<String, Object> baseCorps(final String telephone) {
        final Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("messaging_product", "whatsapp");
        corps.put("recipient_type", "individual");
        corps.put("to", telephone);
        return corps;
    }

    private <T> T executerAvecRetry(final Supplier<T> action) {
        final RetryProperties r = properties.retry();
        int tentative = 0;
        long delai = r.initialDelay().toMillis();
        while (true) {
            tentative++;
            try {
                return action.get();
            } catch (WhatsAppException e) {
                throw e;
            } catch (RestClientResponseException e) {
                if (estTransitoire(e)) {
                    if (tentative >= r.maxAttempts()) {
                        throw traducteur.traduire(e);
                    }
                    delai = dormirAvecRetryAfter(r, delai, e);
                } else {
                    throw traducteur.traduire(e);
                }
            } catch (RestClientException e) {
                // Timeout, connexion interrompue ou corps illisible : transitoire.
                if (tentative >= r.maxAttempts()) {
                    throw new WhatsAppException("Meta injoignable apres " + tentative + " tentatives", e);
                }
                delai = dormir(r, delai);
            }
        }
    }

    private static boolean estTransitoire(final RestClientResponseException e) {
        final int statut = e.getStatusCode().value();
        return statut >= 500 || statut == 429;
    }

    private long dormir(final RetryProperties r, final long delai) {
        try {
            Thread.sleep(delai);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WhatsAppException("Interrompu pendant la tentative WhatsApp", e);
        }
        return Math.min((long) (delai * r.multiplier()), r.maxDelay().toMillis());
    }

    private long dormirAvecRetryAfter(final RetryProperties r, final long delai,
                                      final RestClientResponseException e) {
        final long retryAfter = retryAfterSecondes(e) * 1000;
        try {
            Thread.sleep(retryAfter > 0 ? retryAfter : delai);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WhatsAppException("Interrompu pendant la tentative WhatsApp", ex);
        }
        return Math.min((long) (delai * r.multiplier()), r.maxDelay().toMillis());
    }

    private long retryAfterSecondes(final RestClientResponseException e) {
        final String valeur = e.getResponseHeaders() == null
                ? null
                : e.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (valeur == null || valeur.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(valeur.trim());
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    record ReponseEnvoi(List<MessageEnvoye> messages) {
    }

    record MessageEnvoye(String id) {
    }

    record MediaMeta(String url, String mimeType, String id) {
    }
}


