package com.mfish.erp.simulateur;

import com.mfish.erp.conversation.MessagingPort;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de messagerie du simulateur (mode LOCAL) : remplace {@code ChatwootClient} quand le
 * simulateur est actif en mode LOCAL. Les reponses de l'IA sont enregistrees en memoire au lieu
 * d'etre envoyees a Chatwoot.
 */
@Component
@ConditionalOnExpression(
        "'${app.simulateur.active:false}' == 'true' and '${app.simulateur.mode:LOCAL}' != 'CHATWOOT'")
public class SimulateurMessagingPort implements MessagingPort {

    private static final Logger LOG = LoggerFactory.getLogger(SimulateurMessagingPort.class);

    private final SimulateurMessageStore store;
    private final Clock clock;

    public SimulateurMessagingPort(final SimulateurMessageStore store, final Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public void envoyerMessage(final String chatwootConversationId, final String contenu) {
        LOG.info("Simulateur : reponse IA enregistree (conversation {})", chatwootConversationId);
        store.enregistrer(chatwootConversationId, contenu, clock.instant());
    }

    @Override
    public void etiqueterConversation(final String chatwootConversationId, final String etiquette) {
        LOG.info("Simulateur : etiquette '{}' ignoree (conversation {})",
                etiquette, chatwootConversationId);
    }
}
