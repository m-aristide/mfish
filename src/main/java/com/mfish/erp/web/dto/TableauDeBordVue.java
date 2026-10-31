package com.mfish.erp.web.dto;

/**
 * Vue agregee du tableau de bord (montants deja formats).
 */
public record TableauDeBordVue(
        long commandesDuJour,
        long aLivrer,
        long livrees,
        long montantAEncaisser,
        String montantAEncaisserFormate,
        long conversations) {
}
