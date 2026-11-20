package com.mfish.erp.ia;

import java.util.List;

/**
 * Commande extraite par l'IA (partie {@code commande} du JSON).
 *
 * <p>{@code heureSouhaitee} est optionnelle : elle precise l'heure de livraison souhaitee
 * (« 12h », « vers 15h ») et n'est jamais indispensable a l'enregistrement.
 */
public record CommandeIa(
        List<ProduitIa> produits,
        String quartier,
        String adresse,
        String dateSouhaitee,
        String heureSouhaitee,
        boolean complet) {
}
