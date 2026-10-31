package com.mfish.erp.web.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration transverse de la couche web (rendu serveur Thymeleaf).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MonnaieProperties.class)
public class WebConfig {
}
