package com.mfish.erp.webhook;

/**
 * Payload d'un evenement Chatwoot (champs utiles, extraits du JSON).
 */
public record WebhookPayload(
        String event,
        String id,
        String messageType,
        boolean prive,
        String content,
        String conversationId,
        String senderType,
        String contactPhone,
        String contactName) {
}
