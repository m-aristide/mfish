package com.mfish.erp.whatsapp.client;

/**
 * Levee lorsque le numero du destinataire n'est pas joignable (codes 131049, 131053).
 */
public class WhatsAppNumeroNonJoignableException extends WhatsAppException {

    public WhatsAppNumeroNonJoignableException(final String message) {
        super(message);
    }
}
