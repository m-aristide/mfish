package com.mfish.erp.notification;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Formatage de la fenetre horaire de livraison (±30 min, arrondie a 5 min).
 */
public final class FormatteurFenetre {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private FormatteurFenetre() {
    }

    public static String formater(final LocalTime heureEstimee) {
        final LocalTime arrondie = arrondir5minutes(heureEstimee);
        final LocalTime debut = arrondie.minusMinutes(30);
        final LocalTime fin = arrondie.plusMinutes(30);
        return "Votre livraison est prevue entre " + debut.format(HH_MM)
                + " et " + fin.format(HH_MM) + ".";
    }

    private static LocalTime arrondir5minutes(final LocalTime heure) {
        final int minute = heure.getMinute();
        final int arrondie = Math.round(minute / 5.0f) * 5;
        if (arrondie == 60) {
            return heure.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        }
        return heure.withMinute(arrondie).withSecond(0).withNano(0);
    }
}
