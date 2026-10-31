package com.mfish.erp.commande;

/**
 * Levee lorsqu'une commande est invalide (ligne vide, quantite ou prix incorrects).
 */
public class CommandeInvalideException extends RuntimeException {

    public CommandeInvalideException(String message) {
        super(message);
    }
}
