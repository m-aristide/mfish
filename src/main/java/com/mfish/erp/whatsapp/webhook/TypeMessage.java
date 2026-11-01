package com.mfish.erp.whatsapp.webhook;

/**
 * Types de messages WhatsApp geres (entrants et sortants).
 */
public enum TypeMessage {
    TEXTE,
    IMAGE,
    AUDIO,
    DOCUMENT,
    LOCALISATION,
    CONTACT,
    BOUTON,
    REPONSE_INTERACTIVE,
    REACTION,
    NON_SUPPORTE,
    INCONNU
}
