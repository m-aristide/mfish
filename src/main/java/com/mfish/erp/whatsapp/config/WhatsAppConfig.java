package com.mfish.erp.whatsapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Beans d'infrastructure WhatsApp, crees uniquement quand {@code whatsapp.enabled=true}.
 * En mode desactive, aucun client ni endpoint n'est enregistre (l'application demarre sans lui).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppConfig {

    @Bean
    public RestClient whatsappRestClient(final RestClient.Builder builder,
                                         final WhatsAppProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(final WhatsAppProperties props) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        return factory;
    }
}
