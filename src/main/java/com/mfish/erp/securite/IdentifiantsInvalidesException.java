package com.mfish.erp.securite;

/**
 * Levee lorsque les identifiants de connexion sont invalides.
 */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("Identifiants invalides");
    }
}
