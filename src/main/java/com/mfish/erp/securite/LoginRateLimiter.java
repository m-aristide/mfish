package com.mfish.erp.securite;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Limiteur de tentatives de connexion (fenetre glissante en memoire, par cle).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TENTATIVES = 5;
    private static final Duration FENETRE = Duration.ofMinutes(1);

    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> tentatives = new ConcurrentHashMap<>();

    public LoginRateLimiter(final Clock clock) {
        this.clock = clock;
    }

    /**
     * Indique si une nouvelle tentative est autorisee pour la cle donnee.
     */
    public boolean autorise(final String cle) {
        final Instant maintenant = clock.instant();
        final Deque<Instant> file = tentatives.computeIfAbsent(cle, k -> new ArrayDeque<>());
        synchronized (file) {
            while (!file.isEmpty() && file.peekFirst().isBefore(maintenant.minus(FENETRE))) {
                file.pollFirst();
            }
            if (file.size() >= MAX_TENTATIVES) {
                return false;
            }
            file.addLast(maintenant);
            return true;
        }
    }
}
