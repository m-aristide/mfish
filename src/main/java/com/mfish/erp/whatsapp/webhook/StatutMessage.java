package com.mfish.erp.whatsapp.webhook;

/**
 * Statut d'un message WhatsApp (recu = entrant, envoyes/livres/lus/echoues = sortant).
 */
public enum StatutMessage {
    RECU,
    ENVOYE,
    LIVRE,
    LU,
    ECHEC
}
