package com.mfish.erp.web.dto;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Vue d'un arret de tournee (coordonnees du client et montants formats).
 */
public record ArretVue(
        UUID id,
        UUID commandeId,
        int position,
        LocalTime heureEstimee,
        String statut,
        String clientNom,
        String clientTelephone,
        String quartierNom,
        String adresse,
        long montantTotal,
        String montantTotalFormate,
        long restant,
        String restantFormate) {
}
