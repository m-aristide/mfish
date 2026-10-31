package com.mfish.erp.ia;

/**
 * Levee lorsqu'un quartier indique par l'IA est inconnu ou inactif.
 */
public class QuartierInconnuException extends RuntimeException {

    public QuartierInconnuException(final String nom) {
        super("Quartier inconnu : " + nom);
    }
}
