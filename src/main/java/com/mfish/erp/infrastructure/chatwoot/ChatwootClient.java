package com.mfish.erp.infrastructure.chatwoot;

import com.mfish.erp.config.ChatwootProperties;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.conversation.MessagingPort;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur Chatwoot : envoi de messages et etiquetage de conversations (API v1).
 * Timeout, retry, journalisation sans fuite de jeton.
 */
@Component
@ConditionalOnExpression(
        "'${app.simulateur.active:false}' == 'false' or '${app.simulateur.mode:LOCAL}' == 'CHATWOOT'")
public class ChatwootClient implements MessagingPort {

    private static final Logger LOG = LoggerFactory.getLogger(ChatwootClient.class);

    private final RestClient restClient;
    private final ChatwootProperties properties;
    private final RetrySupport retrySupport;

    public ChatwootClient(final RestClient.Builder builder,
                          final ChatwootProperties properties,
                          final RetrySupport retrySupport) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .defaultHeader("api_access_token", properties.apiToken())
                .build();
        this.retrySupport = retrySupport;
    }

    @Override
    public void envoyerMessage(final String chatwootConversationId, final String contenu) {
        LOG.info("Envoi d'un message Chatwoot (conversation {})", chatwootConversationId);
        retrySupport.executer(properties.retry(), () -> restClient.post()
                .uri("/api/v1/accounts/{accountId}/conversations/{conversationId}/messages",
                        properties.accountId(), chatwootConversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("content", contenu, "message_type", "outgoing"))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void etiqueterConversation(final String chatwootConversationId, final String etiquette) {
        LOG.info("Etiquetage de la conversation Chatwoot {}", chatwootConversationId);
        retrySupport.executer(properties.retry(), () -> restClient.post()
                .uri("/api/v1/accounts/{accountId}/conversations/{conversationId}/labels",
                        properties.accountId(), chatwootConversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("labels", List.of(etiquette)))
                .retrieve()
                .toBodilessEntity());
    }

    private static SimpleClientHttpRequestFactory requestFactory(final ChatwootProperties props) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        return factory;
    }
}
