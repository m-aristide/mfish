package com.mfish.erp.infrastructure.deepseek;

import com.mfish.erp.commun.AppelExterneException;
import com.mfish.erp.config.DeepSeekProperties;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.ia.LlmPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur DeepSeek (API compatible OpenAI). Timeout, retry, journalisation sans fuite de cle.
 */
@Component
public class DeepSeekClient implements LlmPort {

    private static final Logger LOG = LoggerFactory.getLogger(DeepSeekClient.class);

    private final RestClient restClient;
    private final DeepSeekProperties properties;
    private final RetrySupport retrySupport;

    public DeepSeekClient(final RestClient.Builder builder,
                          final DeepSeekProperties properties,
                          final RetrySupport retrySupport) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
        this.retrySupport = retrySupport;
    }

    @Override
    public String generer(final String systemPrompt, final List<String> messages) {
        final DeepSeekRequest request = new DeepSeekRequest(properties.model(),
                List.of(new DeepSeekMessage("system", systemPrompt),
                        new DeepSeekMessage("user", String.join("\n", messages))));
        LOG.info("Appel DeepSeek avec {} messages", messages.size());
        return retrySupport.executer(properties.retry(), () -> {
            final DeepSeekResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new AppelExterneException("DeepSeek a repondu " + res.getStatusCode());
                    })
                    .body(DeepSeekResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AppelExterneException("Reponse DeepSeek invalide");
            }
            return response.choices().get(0).message().content();
        });
    }

    private static SimpleClientHttpRequestFactory requestFactory(final DeepSeekProperties props) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        return factory;
    }

    record DeepSeekRequest(String model, List<DeepSeekMessage> messages) {
    }

    record DeepSeekMessage(String role, String content) {
    }

    record DeepSeekResponse(List<DeepSeekChoice> choices) {
    }

    record DeepSeekChoice(DeepSeekMessage message) {
    }
}
