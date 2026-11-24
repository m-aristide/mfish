package com.mfish.erp.webhook;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.conversation.Conversation;
import com.mfish.erp.conversation.ConversationRepository;
import com.mfish.erp.conversation.EtatCommandeJson;
import com.mfish.erp.conversation.HistoriqueConversation;
import com.mfish.erp.conversation.MessagingPort;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.conversation.RegistreMessagesEmis;
import com.mfish.erp.ia.OrchestrateurIa;
import com.mfish.erp.ia.ResultatIa;
import com.mfish.erp.whatsapp.webhook.DirectionMessage;
import com.mfish.erp.whatsapp.webhook.StatutMessage;
import com.mfish.erp.whatsapp.webhook.TypeMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessageRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/**
 * Traitement asynchrone d'un message entrant : bascule IA/humain, appel du module IA (avec
 * historique et memoire de commande), reponse et persistance de l'etat de la conversation.
 */
@Service
public class TraitementMessageService {

    private static final String ETIQUETTE_REPRENDRE_MAIN = "reprendre-main";

    private final ConversationRepository conversationRepository;
    private final ClientRepository clientRepository;
    private final OrchestrateurIa orchestrateurIa;
    private final MessagingPort messagingPort;
    private final RegistreMessagesEmis registreMessagesEmis;
    private final HistoriqueConversation historiqueConversation;
    private final WhatsAppMessageRepository messageRepository;
    private final Clock clock;

    /**
     * Verrous par conversation : les messages d'une meme conversation sont traites sequentiellement
     * (le traitement est asynchrone sur le {@code TaskExecutor} a threads virtuels). Cela evite les
     * conflits de mise a jour de l'etat de commande lorsque le client envoie plusieurs messages
     * rapproches.
     */
    private final ConcurrentMap<String, Object> verrousConversation = new ConcurrentHashMap<>();

    public TraitementMessageService(final ConversationRepository conversationRepository,
                                    final ClientRepository clientRepository,
                                    final OrchestrateurIa orchestrateurIa,
                                    final MessagingPort messagingPort,
                                    final RegistreMessagesEmis registreMessagesEmis,
                                    final HistoriqueConversation historiqueConversation,
                                    final WhatsAppMessageRepository messageRepository,
                                    final Clock clock) {
        this.conversationRepository = conversationRepository;
        this.clientRepository = clientRepository;
        this.orchestrateurIa = orchestrateurIa;
        this.messagingPort = messagingPort;
        this.registreMessagesEmis = registreMessagesEmis;
        this.historiqueConversation = historiqueConversation;
        this.messageRepository = messageRepository;
        this.clock = clock;
    }

    public void traiter(final WebhookPayload payload) {
        if (payload.prive()) {
            return;
        }
        if ("outgoing".equals(payload.messageType())) {
            // Chatwoot rediffuse aussi les messages sortants de l'application : sans ce filtre,
            // la reponse de l'IA serait prise pour une prise en main humaine et la conversation
            // basculerait en HUMAIN, rendant l'IA muette pour tous les messages suivants.
            final boolean echoApplication =
                    registreMessagesEmis.consommerEcho(payload.conversationId(), payload.content());
            if ("user".equalsIgnoreCase(payload.senderType()) && !echoApplication) {
                basculerEnHumain(payload.conversationId());
            }
            return;
        }

        final String conversationId =
                payload.conversationId() == null ? "" : payload.conversationId();
        final Object verrou = verrousConversation.computeIfAbsent(conversationId, k -> new Object());
        synchronized (verrou) {
            traiterSousVerrou(payload);
        }
    }

    /** Traitement d'un message entrant, execute sous verrou par conversation. */
    private void traiterSousVerrou(final WebhookPayload payload) {
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

        final String texte = payload.content() == null ? "" : payload.content();
        final String wamidEntrant = wamidEntrant(payload);
        enregistrerMessageEntrant(payload, texte);
        final ResultatIa resultat = orchestrateurIa.traiter(client,
                EtatCommandeJson.depuisJson(conversation.getEtatCommandeJson()),
                historiqueConversation.charger(payload.contactPhone(), wamidEntrant),
                texte);
        messagingPort.envoyerMessage(payload.conversationId(), resultat.reponseAuClient());
        enregistrerMessageSortant(payload, resultat.reponseAuClient());
        if (resultat.escalade()) {
            basculerEnHumain(payload.conversationId());
            messagingPort.etiqueterConversation(payload.conversationId(), ETIQUETTE_REPRENDRE_MAIN);
        }

        conversation.setEtatCommandeJson(EtatCommandeJson.versJson(resultat.etat()));
        conversation.setDerniereActivite(clock.instant());
        conversationRepository.save(conversation);
    }

    /** Identifiant local du message entrant : les evenements Chatwoot n'ont pas de wamid. */
    static String wamidEntrant(final WebhookPayload payload) {
        return "chatwoot-" + payload.id();
    }

    private void enregistrerMessageEntrant(final WebhookPayload payload, final String texte) {
        final String wamid = wamidEntrant(payload);
        if (messageRepository.existsByWamid(wamid)) {
            return;
        }
        messageRepository.save(new WhatsAppMessage(wamid, DirectionMessage.ENTRANT,
                payload.contactPhone(), TypeMessage.TEXTE, texte, StatutMessage.RECU, null, null));
    }

    private void enregistrerMessageSortant(final WebhookPayload payload, final String contenu) {
        messageRepository.save(new WhatsAppMessage("chatwoot-out-" + UUID.randomUUID(),
                DirectionMessage.SORTANT, payload.contactPhone(), TypeMessage.TEXTE, contenu,
                StatutMessage.ENVOYE, null, null));
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
