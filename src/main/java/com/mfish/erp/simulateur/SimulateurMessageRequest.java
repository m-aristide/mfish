package com.mfish.erp.simulateur;

/**
 * Corps de la requete d'envoi d'un message depuis l'interface du simulateur.
 */
public record SimulateurMessageRequest(
        String conversationId,
        String telephone,
        String nom,
        String contenu) {
}
