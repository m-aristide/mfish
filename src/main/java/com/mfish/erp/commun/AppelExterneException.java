package com.mfish.erp.commun;

/**
 * Levee lorsqu'un appel a un service externe echoue definitivement.
 */
public class AppelExterneException extends RuntimeException {

    public AppelExterneException(final String message) {
        super(message);
    }

    public AppelExterneException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
