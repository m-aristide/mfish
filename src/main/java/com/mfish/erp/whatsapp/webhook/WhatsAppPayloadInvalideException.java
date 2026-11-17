package com.mfish.erp.whatsapp.webhook;

/**
 * Levee lorsque le corps d'un webhook WhatsApp est malforme.
 */
public class WhatsAppPayloadInvalideException extends RuntimeException {

    public WhatsAppPayloadInvalideException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
