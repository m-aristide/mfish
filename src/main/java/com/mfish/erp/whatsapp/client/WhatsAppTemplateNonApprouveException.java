package com.mfish.erp.whatsapp.client;

/**
 * Levee lorsque le template utilise n'existe pas ou n'est pas approuve
 * (codes 132000, 132001, 132012).
 */
public class WhatsAppTemplateNonApprouveException extends WhatsAppException {

    public WhatsAppTemplateNonApprouveException(final String message) {
        super(message);
    }
}
