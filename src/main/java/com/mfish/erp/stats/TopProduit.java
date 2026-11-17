package com.mfish.erp.stats;

/**
 * Produit le plus vendu (nom + quantite totale).
 */
public record TopProduit(String produit, long quantiteTotale) {
}
