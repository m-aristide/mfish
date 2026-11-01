package com.mfish.erp.whatsapp.client;

/**
 * Levee lorsque la limite de debit de l'API Meta est atteinte (codes 131026, 100, 80007).
 */
public class WhatsAppLimiteDebitException extends WhatsAppException {

    public WhatsAppLimiteDebitException(final String message) {
        super(message);
    }
}
