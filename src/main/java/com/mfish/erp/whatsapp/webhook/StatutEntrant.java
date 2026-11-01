package com.mfish.erp.whatsapp.webhook;

/**
 * Notification de statut d'un message sortant (sent/delivered/read/failed).
 */
public record StatutEntrant(
        String wamid,
        String statut,
        String codeErreur) {
}
