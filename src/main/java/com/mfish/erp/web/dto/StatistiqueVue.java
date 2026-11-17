package com.mfish.erp.web.dto;

import java.util.List;

/**
 * Vue des statistiques de vente sur une periode (montants formats).
 */
public record StatistiqueVue(
        String periode,
        long ca,
        String caFormate,
        long nbCommandes,
        long tauxLivraison,
        List<ProduitStatistique> topProduits,
        List<QuartierStatistique> quartiers) {

    public record ProduitStatistique(String nom, long quantite) {
    }

    public record QuartierStatistique(String nom, long ca, String caFormate) {
    }
}
