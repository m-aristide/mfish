package com.mfish.erp.ia;

import java.util.List;

/**
 * Commande extraite par l'IA (partie {@code commande} du JSON).
 */
public record CommandeIa(
        List<ProduitIa> produits,
        String quartier,
        String adresse,
        String dateSouhaitee,
        boolean complet) {
}
