package com.mfish.erp.simulateur;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Stockage en memoire des messages sortants du simulateur, indexe par identifiant de
 * conversation. Les messages sont produits de facon asynchrone par le pipeline webhook et lus
 * par l'interface (polling) : la structure est donc thread-safe et triee par horodatage.
 */
@Component
public class SimulateurMessageStore {

    private final ConcurrentMap<String, List<SimulateurMessage>> parConversation =
            new ConcurrentHashMap<>();

    public void enregistrer(final String conversationId, final String contenu,
                            final Instant horodatage) {
        parConversation.computeIfAbsent(conversationId, ignored -> new CopyOnWriteArrayList<>())
                .add(new SimulateurMessage(conversationId, contenu, horodatage));
    }

    public List<SimulateurMessage> messages(final String conversationId) {
        final List<SimulateurMessage> messages =
                parConversation.getOrDefault(conversationId, List.of());
        final List<SimulateurMessage> copie = new ArrayList<>(messages);
        copie.sort(Comparator.comparing(SimulateurMessage::horodatage));
        return copie;
    }

    public void effacer(final String conversationId) {
        parConversation.remove(conversationId);
    }
}
