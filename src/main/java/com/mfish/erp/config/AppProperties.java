package com.mfish.erp.config;

import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Proprietes generales de l'application (prefixe {@code app}).
 *
 * <p>Les groupes specifiques (JWT, Chatwoot, DeepSeek, RouteXL, geocodage) sont declares
 * dans leur propre record, a l'etape ou ils sont utilises.
 *
 * @param timezone fuseau horaire metier, utilise pour definir "aujourd'hui" et les heures
 *                 de livraison (variable {@code APP_TIMEZONE})
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotNull ZoneId timezone) {
}
