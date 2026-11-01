package com.mfish.erp.whatsapp.client;

/**
 * Exception metier de base de l'integration WhatsApp Cloud API.
 */
public class WhatsAppException extends RuntimeException {

    public WhatsAppException(final String message) {
        super(message);
    }

    public WhatsAppException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
