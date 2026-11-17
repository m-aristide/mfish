package com.mfish.erp.whatsapp.webhook;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Limitation de debit en memoire par numero emetteur (protection anti-flood).
 * Fenetre glissante simple : au plus {@code MAX_MESSAGES} par minute par numero.
 */
@Component
public class RateLimiterWhatsApp {

    private static final int MAX_MESSAGES = 30;
    private static final Duration FENETRE = Duration.ofMinutes(1);

    private final ConcurrentMap<String, Fenetre> compteurs = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiterWhatsApp(final Clock clock) {
        this.clock = clock;
    }

    public boolean autorise(final String telephone) {
        if (telephone == null || telephone.isBlank()) {
            return true;
        }
        final Instant maintenant = clock.instant();
        final Fenetre fenetre = compteurs.compute(telephone, (cle, ancienne) -> {
            if (ancienne == null || maintenant.isAfter(ancienne.debut().plus(FENETRE))) {
                return new Fenetre(maintenant, 1);
            }
            return new Fenetre(ancienne.debut(), ancienne.compte() + 1);
        });
        return fenetre.compte() <= MAX_MESSAGES;
    }

    private record Fenetre(Instant debut, int compte) {
    }
}
