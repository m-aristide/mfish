package com.mfish.erp.webhook;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.conversation.Conversation;
import com.mfish.erp.conversation.ConversationRepository;
import com.mfish.erp.conversation.MessagingPort;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.ia.OrchestrateurIa;
import com.mfish.erp.ia.ResultatIa;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Traitement asynchrone d'un message entrant : bascule IA/humain, appel du module IA et reponse.
 */
@Service
public class TraitementMessageService {

    private static final String ETIQUETTE_REPRENDRE_MAIN = "reprendre-main";

    private final ConversationRepository conversationRepository;
    private final ClientRepository clientRepository;
    private final OrchestrateurIa orchestrateurIa;
    private final MessagingPort messagingPort;
    private final Clock clock;

    public TraitementMessageService(final ConversationRepository conversationRepository,
                                    final ClientRepository clientRepository,
                                    final OrchestrateurIa orchestrateurIa,
                                    final MessagingPort messagingPort,
                                    final Clock clock) {
        this.conversationRepository = conversationRepository;
        this.clientRepository = clientRepository;
        this.orchestrateurIa = orchestrateurIa;
        this.messagingPort = messagingPort;
        this.clock = clock;
    }

    public void traiter(final WebhookPayload payload) {
        if (payload.prive()) {
            return;
        }
        if ("outgoing".equals(payload.messageType())) {
            if ("user".equals(payload.senderType())) {
                basculerEnHumain(payload.conversationId());
            }
            return;
        }

        final Conversation conversation = trouverOuCreer(payload.conversationId());
        if (conversation.getMode() == ModeConversation.HUMAIN) {
            return;
        }
        if (payload.contactPhone() == null || payload.contactPhone().isBlank()) {
            return;
        }

        final Client client = clientRepository.findByTelephone(payload.contactPhone())
                .orElseGet(() -> clientRepository.save(new Client(
                        payload.contactName() == null ? "Client" : payload.contactName(),
                        payload.contactPhone())));

        final ResultatIa resultat = orchestrateurIa.traiter(client,
                List.of(payload.content()), payload.content());
        messagingPort.envoyerMessage(payload.conversationId(), resultat.reponseAuClient());
        if (resultat.escalade()) {
            basculerEnHumain(payload.conversationId());
            messagingPort.etiqueterConversation(payload.conversationId(), ETIQUETTE_REPRENDRE_MAIN);
        }

        conversation.setDerniereActivite(clock.instant());
        conversationRepository.save(conversation);
    }

    private Conversation trouverOuCreer(final String chatwootConversationId) {
        return conversationRepository.findByChatwootConversationId(chatwootConversationId)
                .orElseGet(() -> conversationRepository.save(
                        new Conversation(chatwootConversationId, ModeConversation.IA)));
    }

    private void basculerEnHumain(final String chatwootConversationId) {
        final Conversation conversation = conversationRepository
                .findByChatwootConversationId(chatwootConversationId)
                .orElseGet(() -> new Conversation(chatwootConversationId, ModeConversation.HUMAIN));
        conversation.setMode(ModeConversation.HUMAIN);
        conversationRepository.save(conversation);
    }
}
