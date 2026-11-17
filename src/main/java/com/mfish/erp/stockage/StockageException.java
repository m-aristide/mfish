package com.mfish.erp.stockage;

/**
 * Erreur de stockage d'un fichier (type non autorise, lecture/ecriture impossible).
 */
public class StockageException extends RuntimeException {

    public StockageException(final String message) {
        super(message);
    }

    public StockageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}