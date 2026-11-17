package com.mfish.erp.ia;

/**
 * Levee lorsqu'un produit demande est ambigu (plusieurs correspondances proches).
 */
public class ProduitAmbiguException extends RuntimeException {

    public ProduitAmbiguException(final String nom) {
        super("Produit ambigu : " + nom);
    }
}
