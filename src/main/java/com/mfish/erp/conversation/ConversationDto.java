package com.mfish.erp.conversation;

import java.time.Instant;
import java.util.UUID;

/**
 * Representation publique d'une conversation WhatsApp.
 */
public record ConversationDto(
        UUID id,
        String chatwootConversationId,
        UUID clientId,
        String clientNom,
        String clientTelephone,
        ModeConversation mode,
        Instant derniereActivite) {

    public static ConversationDto from(final Conversation conversation) {
        return new ConversationDto(
                conversation.getId(),
                conversation.getChatwootConversationId(),
                conversation.getClient() == null ? null : conversation.getClient().getId(),
                conversation.getClient() == null ? null : conversation.getClient().getNom(),
                conversation.getClient() == null ? null : conversation.getClient().getTelephone(),
                conversation.getMode(),
                conversation.getDerniereActivite());
    }
}
