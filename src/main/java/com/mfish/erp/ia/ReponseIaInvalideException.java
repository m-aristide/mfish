package com.mfish.erp.ia;

/**
 * Levee lorsque la sortie JSON de l'IA est invalide ou malformee.
 */
public class ReponseIaInvalideException extends RuntimeException {

    public ReponseIaInvalideException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
