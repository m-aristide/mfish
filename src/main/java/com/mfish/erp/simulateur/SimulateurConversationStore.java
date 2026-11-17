package com.mfish.erp.simulateur;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Associe un identifiant de conversation du simulateur a une conversation Chatwoot (mode
 * CHATWOOT). Les associations sont en memoire et perdues au redemarrage.
 */
@Component
public class SimulateurConversationStore {

    private final ConcurrentMap<String, SimulateurConversation> parSimulation =
            new ConcurrentHashMap<>();

    public Optional<SimulateurConversation> trouver(final String conversationId) {
        return Optional.ofNullable(parSimulation.get(conversationId));
    }

    public void enregistrer(final String conversationId, final SimulateurConversation reference) {
        parSimulation.put(conversationId, reference);
    }

    public void effacer(final String conversationId) {
        parSimulation.remove(conversationId);
    }
}
