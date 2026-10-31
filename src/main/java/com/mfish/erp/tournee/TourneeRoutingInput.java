package com.mfish.erp.tournee;

import java.time.LocalTime;
import java.util.List;

/**
 * Entree d'une optimisation de tournee.
 */
public record TourneeRoutingInput(
        Coordonnees depart,
        LocalTime heureDepart,
        List<ArretAOptimiser> arrets) {
}
