package com.mfish.erp.whatsapp.client;

/**
 * Levee lorsque le jeton d'acces Graph API est expire, revoque ou invalide (code 190).
 */
public class WhatsAppTokenInvalideException extends WhatsAppException {

    public WhatsAppTokenInvalideException(final String message) {
        super(message);
    }
}
