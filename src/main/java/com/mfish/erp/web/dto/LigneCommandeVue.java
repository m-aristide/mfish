package com.mfish.erp.web.dto;

import java.util.UUID;

/**
 * Vue d'une ligne de commande (nom du produit et montants formats).
 */
public record LigneCommandeVue(
        UUID id,
        String produitNom,
        int quantite,
        long prixUnitaire,
        String prixUnitaireFormate,
        long sousTotal,
        String sousTotalFormate) {
}
