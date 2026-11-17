package com.mfish.erp.whatsapp.client;

/**
 * Levee lorsque le destinataire n'est pas joignable ou n'est pas autorise en mode test
 * (code 131030 : recipient n'est pas autorise).
 */
public class WhatsAppDestinataireInvalideException extends WhatsAppException {

    public WhatsAppDestinataireInvalideException(final String message) {
        super(message);
    }
}
