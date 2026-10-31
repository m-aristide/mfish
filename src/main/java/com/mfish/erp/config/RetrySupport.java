package com.mfish.erp.config;

import java.util.function.Supplier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Execution d'une action avec retry et backoff exponentiel (Spring Retry), sans AOP.
 */
@Component
public class RetrySupport {

    /**
     * Execute {@code action} et retente en cas d'exception selon la politique donnee.
     */
    public <T> T executer(final RetryProperties properties, final Supplier<T> action) {
        final RetryTemplate template = RetryTemplate.builder()
                .maxAttempts(properties.maxAttempts())
                .exponentialBackoff(properties.initialDelay().toMillis(), properties.multiplier(),
                        properties.maxDelay().toMillis())
                .retryOn(Exception.class)
                .build();
        return template.execute(ctx -> action.get());
    }
}
