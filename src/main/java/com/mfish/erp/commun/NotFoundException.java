package com.mfish.erp.commun;

/**
 * Levee lorsqu'une ressource demandee n'existe pas.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(final String message) {
        super(message);
    }
}
