package com.mfish.erp.whatsapp.client;

/**
 * Levee lorsqu'un message libre est envoye hors de la fenetre de service de 24 h
 * (code 131047 : re-engagement requis, un template est necessaire).
 */
public class WhatsAppHorsFenetreException extends WhatsAppException {

    public WhatsAppHorsFenetreException(final String message) {
        super(message);
    }
}
