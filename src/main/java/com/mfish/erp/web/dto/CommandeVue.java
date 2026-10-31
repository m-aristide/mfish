package com.mfish.erp.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vue d'une commande pour les gabarits (noms et montants deja formats).
 */
public record CommandeVue(
        UUID id,
        String clientNom,
        String clientTelephone,
        String quartierNom,
        String statut,
        LocalDate date,
        String modePaiement,
        String statutPaiement,
        long montantTotal,
        String montantTotalFormate,
        long montantPaye,
        String montantPayeFormate,
        long restant,
        String restantFormate,
        String notes,
        String source,
        List<LigneCommandeVue> lignes) {
}
