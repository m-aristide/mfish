package com.mfish.erp.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Regle des 24 h de WhatsApp : un message libre ne peut etre envoye que dans les 24 h
 * suivant la derniere activite du client. Au-dela, un template doit etre utilise.
 */
public final class Regle24Heures {

    public static final Duration FENETRE = Duration.ofHours(24);

    private Regle24Heures() {
    }

    public static boolean peutEnvoyerMessageLibre(final Instant derniereActivite, final Clock clock) {
        if (derniereActivite == null) {
            return false;
        }
        return !clock.instant().isAfter(derniereActivite.plus(FENETRE));
    }
}
