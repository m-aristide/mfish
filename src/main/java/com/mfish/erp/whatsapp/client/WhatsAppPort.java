package com.mfish.erp.whatsapp.client;

import java.util.List;

/**
 * Port de messagerie WhatsApp Cloud API (implemente par {@link WhatsAppClient}).
 * Le code metier ne depend que de cette interface (jamais de RestClient directement).
 */
public interface WhatsAppPort {

    /** Envoie un message texte libre (decoupe automatiquement si necessaire). Retourne le wamid. */
    String envoyerTexte(String telephone, String contenu);

    /** Envoie un template (obligatoire hors fenetre de 24 h). Retourne le wamid. */
    String envoyerTemplate(String telephone, String nom, String langue, List<String> variablesCorps);

    /** Envoie un message interactif avec boutons de reponse rapide. Retourne le wamid. */
    String envoyerReponseInteractive(String telephone, String corps, List<String> options);

    /** Marque un message entrant comme lu ({@code status: read}). */
    void marquerLu(String telephone, String wamid);

    /** Indicateur de saisie (best-effort, non documente dans la Cloud API). */
    void indicateurSaisie(String telephone);

    /** Telecharge un media entrant a partir de son identifiant Meta. */
    MediaTelecharge telechargerMedia(String mediaId);
}
