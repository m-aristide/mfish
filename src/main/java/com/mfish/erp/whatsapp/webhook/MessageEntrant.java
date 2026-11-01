package com.mfish.erp.whatsapp.webhook;

/**
 * Message entrant extrait d'un webhook WhatsApp (champs utiles, sans donnees sensibles).
 */
public record MessageEntrant(
        String wamid,
        String telephone,
        String nomContact,
        TypeMessage type,
        String texte,
        String mediaId) {
}
