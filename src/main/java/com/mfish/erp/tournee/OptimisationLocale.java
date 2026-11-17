package com.mfish.erp.tournee;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Plan B local : ordonnancement sans service externe. Les arrets sont d'abord groupes par
 * {@code ordreQuartier} (ordre par defaut), puis ordonnes par plus proche voisin (Haversine).
 * Les heures d'arrivee sont estimees par une vitesse moyenne et une duree d'arret.
 */
public final class OptimisationLocale {

    private OptimisationLocale() {
    }

    public static List<ArretPlanifie> optimiser(final TourneeRoutingInput input,
                                                final double vitesseKmh,
                                                final int dureeArretMinutes) {
        Objects.requireNonNull(input, "input");
        if (vitesseKmh <= 0) {
            throw new IllegalArgumentException("La vitesse moyenne doit etre strictement positive");
        }
        if (dureeArretMinutes < 0) {
            throw new IllegalArgumentException("La duree d'arret ne peut pas etre negative");
        }

        final List<ArretAOptimiser> restants = new ArrayList<>(input.arrets());
        final List<ArretPlanifie> resultat = new ArrayList<>();
        Coordonnees courant = input.depart();
        LocalTime heure = input.heureDepart();
        int position = 0;

        while (!restants.isEmpty()) {
            final int ordreMinimum = restants.stream()
                    .mapToInt(ArretAOptimiser::ordreQuartier).min().orElseThrow();
            final ArretAOptimiser suivant = plusProche(courant, restants, ordreMinimum);
            restants.remove(suivant);

            final double distanceKm = DistanceHaversine.kilometres(courant, suivant.coordonnees());
            final long trajetMinutes = Math.round(distanceKm / vitesseKmh * 60.0);
            heure = heure.plusMinutes(trajetMinutes);
            resultat.add(new ArretPlanifie(suivant.commandeId(), position++, heure));
            heure = heure.plusMinutes(dureeArretMinutes);
            courant = suivant.coordonnees();
        }
        return resultat;
    }

    private static ArretAOptimiser plusProche(final Coordonnees point,
                                              final List<ArretAOptimiser> candidats,
                                              final int ordre) {
        return candidats.stream()
                .filter(arret -> arret.ordreQuartier() == ordre)
                .min(Comparator.comparingDouble(
                        arret -> DistanceHaversine.kilometres(point, arret.coordonnees())))
                .orElseThrow();
    }
}
