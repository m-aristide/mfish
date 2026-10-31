package com.mfish.erp.ia;

/**
 * Levee lorsqu'un produit demande par l'IA est inconnu du catalogue.
 */
public class ProduitInconnuException extends RuntimeException {

    public ProduitInconnuException(final String nom) {
        super("Produit inconnu : " + nom);
    }
}
