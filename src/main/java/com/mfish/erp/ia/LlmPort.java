package com.mfish.erp.ia;

import java.util.List;

/**
 * Port d'appel au modele de langage (implemente par l'adaptateur DeepSeek).
 */
public interface LlmPort {

    /**
     * Genere une reponse a partir d'un prompt systeme et d'un historique de messages.
     * Retourne le texte brut (JSON strict attendu).
     */
    String generer(String systemPrompt, List<String> messages);
}
