package com.mfish.erp.conversation;

/**
 * Port de messagerie (boite partagee) : envoyer des messages et etiqueter des conversations.
 * Implemente par l'adaptateur Chatwoot.
 */
public interface MessagingPort {

    void envoyerMessage(String chatwootConversationId, String contenu);

    void etiqueterConversation(String chatwootConversationId, String etiquette);
}
