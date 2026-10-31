package com.mfish.erp.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration transverse : proprietes typees et horloge injectable.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        AppProperties.class,
        JwtProperties.class,
        AdminProperties.class,
        CorsProperties.class,
        ChatwootProperties.class,
        DeepSeekProperties.class,
        RoutexlProperties.class,
        GeocodingProperties.class
})
public class ApplicationConfig {

    /**
     * Horloge unique de l'application, calee sur le fuseau metier. Le code metier ne doit
     * jamais appeler {@code Instant.now()} directement : il injecte ce bean.
     */
    @Bean
    public Clock clock(final AppProperties properties) {
        return Clock.system(properties.timezone());
    }
}
