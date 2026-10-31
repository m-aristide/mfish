package com.mfish.erp.ia;

/**
 * Produit demande par l'IA (nom libre + quantite). Jamais utilise directement : le nom est
 * rapproche du catalogue reel et le prix est lu en base.
 */
public record ProduitIa(String nom, int quantite) {
}
