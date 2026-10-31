package com.mfish.erp.tournee;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Arret planifie (position dans la tournee et heure d'arrivee estimee).
 */
public record ArretPlanifie(UUID commandeId, int position, LocalTime heureArrivee) {
}
