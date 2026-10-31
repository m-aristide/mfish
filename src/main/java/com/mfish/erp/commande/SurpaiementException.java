package com.mfish.erp.commande;

/**
 * Levee lors d'une tentative de surpaiement d'une commande.
 */
public class SurpaiementException extends RuntimeException {

    public SurpaiementException(long montantTotal, long montantPaye) {
        super("Surpaiement refuse : total=" + montantTotal + ", paye=" + montantPaye);
    }
}
