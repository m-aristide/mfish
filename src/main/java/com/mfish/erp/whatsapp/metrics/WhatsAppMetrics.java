package com.mfish.erp.whatsapp.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Compteurs et latence de l'integration WhatsApp (Micrometer). Aucune donnee personnelle.
 */
@Component
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppMetrics {

    private final Counter messagesEntrants;
    private final Counter messagesSortants;
    private final Counter echecs;
    private final Counter signaturesRejetees;
    private final Counter doublons;
    private final Counter rateLimites;
    private final Timer latenceMeta;

    public WhatsAppMetrics(final MeterRegistry registry) {
        this.messagesEntrants = registry.counter("whatsapp.messages.entrants");
        this.messagesSortants = registry.counter("whatsapp.messages.sortants");
        this.echecs = registry.counter("whatsapp.echecs");
        this.signaturesRejetees = registry.counter("whatsapp.signatures.rejetees");
        this.doublons = registry.counter("whatsapp.doublons");
        this.rateLimites = registry.counter("whatsapp.rate.limites");
        this.latenceMeta = registry.timer("whatsapp.meta.latence");
    }

    public void incrementerEntrants() {
        messagesEntrants.increment();
    }

    public void incrementerSortants() {
        messagesSortants.increment();
    }

    public void incrementerEchecs() {
        echecs.increment();
    }

    public void incrementerSignaturesRejetees() {
        signaturesRejetees.increment();
    }

    public void incrementerDoublons() {
        doublons.increment();
    }

    public void incrementerRateLimites() {
        rateLimites.increment();
    }

    public <T> T mesurerMeta(final Supplier<T> action) {
        return latenceMeta.record(action);
    }
}
