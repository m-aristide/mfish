package com.mfish.erp.conversation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Registre des messages emis par l'application vers Chatwoot.
 *
 * <p>Chatwoot rediffuse un webhook {@code message_created} pour <em>tous</em> les messages, y
 * compris les messages sortants postes par l'application avec son jeton d'API. Sans ce registre,
 * le traitement du webhook prend la reponse de l'IA pour une prise en main par un agent humain et
 * bascule la conversation en mode {@code HUMAIN} : l'IA devient alors muette pour tous les
 * messages suivants (symptome « une seule reponse puis plus rien »).</p>
 *
 * <p>Un message emis est memorise avant l'appel a Chatwoot et l'entree est consommee au premier
 * webhook d'echo (contenu identique, espaces normalises). Une reponse d'agent reellement humaine,
 * dont le contenu ne correspond a aucune entree, conserve donc son effet de bascule. Les entrees
 * non consommees periment au bout de {@link #PEREMPTION}.</p>
 */
@Component
public class RegistreMessagesEmis {

    private static final Duration PEREMPTION = Duration.ofMinutes(10);

    private final ConcurrentMap<String, List<Entree>> parConversation = new ConcurrentHashMap<>();
    private final Clock clock;

    public RegistreMessagesEmis(final Clock clock) {
        this.clock = clock;
    }

    /**
     * Memorise un message sur le point d'etre envoye a Chatwoot. L'appel a lieu avant l'envoi
     * HTTP : le webhook d'echo peut arriver avant le retour de l'API.
     */
    public void enregistrer(final String conversationId, final String contenu) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        final String cle = normaliser(contenu);
        if (cle.isEmpty()) {
            return;
        }
        final List<Entree> entrees = parConversation.computeIfAbsent(conversationId,
                ignored -> new CopyOnWriteArrayList<>());
        purger(entrees);
        entrees.add(new Entree(cle, clock.instant().plus(PEREMPTION)));
    }

    /** Oublie un message dont l'envoi a echoue : Chatwoot ne rediffusera aucun echo. */
    public void oublier(final String conversationId, final String contenu) {
        final List<Entree> entrees = conversationId == null ? null : parConversation.get(conversationId);
        if (entrees == null) {
            return;
        }
        final String cle = normaliser(contenu);
        entrees.stream()
                .filter(entree -> entree.contenu().equals(cle))
                .findFirst()
                .ifPresent(entrees::remove);
    }

    /**
     * Indique si le contenu correspond a un message emis par l'application (webhook d'echo).
     * L'entree est consommee : un meme contenu envoye deux fois ne masque qu'un seul echo.
     */
    public boolean consommerEcho(final String conversationId, final String contenu) {
        final List<Entree> entrees = conversationId == null ? null : parConversation.get(conversationId);
        if (entrees == null) {
            return false;
        }
        purger(entrees);
        final String cle = normaliser(contenu);
        for (final Entree entree : entrees) {
            if (entree.contenu().equals(cle)) {
                entrees.remove(entree);
                return true;
            }
        }
        return false;
    }

    private void purger(final List<Entree> entrees) {
        final Instant maintenant = clock.instant();
        entrees.removeIf(entree -> !entree.expireLe().isAfter(maintenant));
    }

    private static String normaliser(final String contenu) {
        return contenu == null ? "" : contenu.replaceAll("\\s+", " ").trim();
    }

    private record Entree(String contenu, Instant expireLe) {
    }
}
