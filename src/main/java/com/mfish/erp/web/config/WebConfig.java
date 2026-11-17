package com.mfish.erp.web.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration transverse de la couche web (rendu serveur Thymeleaf).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MonnaieProperties.class)
public class WebConfig {

    /**
     * Sert les fichiers televerses depuis le repertoire local {@code app.stockage.uploads-dir}
     * sous l'URL {@code /uploads/**}.
     */
    @Bean
    public WebMvcConfigurer uploadsConfig(
            @Value("${app.stockage.uploads-dir:uploads}") final String uploadsDir) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(final ResourceHandlerRegistry registry) {
                final String emplacement = Path.of(uploadsDir).toAbsolutePath().normalize()
                        .toFile().getAbsolutePath();
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + emplacement + "/");
            }
        };
    }
}
