package com.mfish.erp.whatsapp.service;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.conversation.Conversation;
import com.mfish.erp.conversation.ConversationRepository;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.ia.OrchestrateurIa;
import com.mfish.erp.ia.ResultatIa;
import com.mfish.erp.whatsapp.Masquage;
import com.mfish.erp.whatsapp.client.WhatsAppPort;
import com.mfish.erp.whatsapp.metrics.WhatsAppMetrics;
import com.mfish.erp.whatsapp.webhook.DirectionMessage;
import com.mfish.erp.whatsapp.webhook.MessageEntrant;
import com.mfish.erp.whatsapp.webhook.StatutMessage;
import com.mfish.erp.whatsapp.webhook.TypeMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessageRepository;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration d'une conversation WhatsApp : normalisation E.164, client/conversation,
 * opt-out, mode humain, appel IA et reponse, escalade en cas d'echec.
 */
@Service
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppConversationService {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsAppConversationService.class);
    private static final String MESSAGE_OPTOUT =
            "Votre demande a bien ete prise en compte. Vous ne recevrez plus de messages de notre part.";

    private final ClientRepository clientRepository;
    private final ConversationRepository conversationRepository;
    private final WhatsAppPort whatsAppPort;
    private final OrchestrateurIa orchestrateurIa;
    private final WhatsAppMessageRepository messageRepository;
    private final WhatsAppMetrics metrics;
    private final Clock clock;

    public WhatsAppConversationService(final ClientRepository clientRepository,
                                       final ConversationRepository conversationRepository,
                                       final WhatsAppPort whatsAppPort,
                                       final OrchestrateurIa orchestrateurIa,
                                       final WhatsAppMessageRepository messageRepository,
                                       final WhatsAppMetrics metrics,
                                       final Clock clock) {
        this.clientRepository = clientRepository;
        this.conversationRepository = conversationRepository;
        this.whatsAppPort = whatsAppPort;
        this.orchestrateurIa = orchestrateurIa;
        this.messageRepository = messageRepository;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public void traiter(final MessageEntrant message) {
        final String telephone = NormaliseurTelephone.normaliser(message.telephone());
        if (telephone == null) {
            return;
        }
        final String texte = message.texte() == null ? "" : message.texte();
        if (DetecteurOptOut.estOpposition(texte)) {
            desinscrireEtConfirmer(telephone, message.nomContact());
            return;
        }

        final Client client = trouverOuCreerClient(telephone, message.nomContact());
        final Conversation conversation = trouverOuCreer(telephone, client);
        conversation.setDerniereActivite(clock.instant());
        conversationRepository.save(conversation);

        marquerLu(telephone, message.wamid());

        if (conversation.getMode() == ModeConversation.HUMAIN) {
            LOG.info("Conversation en mode humain, pas de reponse IA pour {}",
                    Masquage.telephone(telephone));
            return;
        }
        repondreParIa(client, conversation, telephone, texte);
    }

    /** Envoie un template proactif uniquement si le client est joignable et n'a pas fait opposition. */
    @Transactional
    public boolean envoyerTemplateSiContactable(final Client client, final String nom,
                                                final String langue, final List<String> variables) {
        if (!client.isContactable()) {
            return false;
        }
        final String wamid = whatsAppPort.envoyerTemplate(client.getTelephone(), nom, langue, variables);
        messageRepository.save(messageSortant(client.getTelephone(), wamid, nom, TypeMessage.TEXTE, nom));
        return true;
    }

    private void desinscrireEtConfirmer(final String telephone, final String nom) {
        final Client client = trouverOuCreerClient(telephone, nom);
        client.setContactable(false);
        clientRepository.save(client);
        try {
            final String wamid = whatsAppPort.envoyerTexte(telephone, MESSAGE_OPTOUT);
            messageRepository.save(messageSortant(telephone, wamid, MESSAGE_OPTOUT, TypeMessage.TEXTE, null));
        } catch (Exception e) {
            LOG.warn("Echec de la confirmation d'opt-out pour {}", Masquage.telephone(telephone));
        }
    }

    private void repondreParIa(final Client client, final Conversation conversation,
                               final String telephone, final String texte) {
        try {
            final ResultatIa resultat = orchestrateurIa.traiter(client, List.of(), texte);
            if (!client.isContactable()) {
                return;
            }
            final String wamid = whatsAppPort.envoyerTexte(telephone, resultat.reponseAuClient());
            messageRepository.save(messageSortant(telephone, wamid, resultat.reponseAuClient(),
                    TypeMessage.TEXTE, null));
            if (resultat.escalade()) {
                basculerHumain(conversation);
            }
        } catch (Exception e) {
            LOG.warn("Echec de la reponse IA, escalade pour {}", Masquage.telephone(telephone));
            metrics.incrementerEchecs();
            basculerHumain(conversation);
        }
    }

    private void marquerLu(final String telephone, final String wamid) {
        try {
            whatsAppPort.marquerLu(telephone, wamid);
        } catch (Exception e) {
            LOG.warn("Impossible de marquer comme lu pour {}", Masquage.telephone(telephone));
        }
    }

    private Client trouverOuCreerClient(final String telephone, final String nom) {
        return clientRepository.findByTelephone(telephone)
                .orElseGet(() -> clientRepository.save(new Client(
                        nom == null || nom.isBlank() ? "Client WhatsApp" : nom, telephone)));
    }

    private Conversation trouverOuCreer(final String telephone, final Client client) {
        return conversationRepository.findByTelephone(telephone)
                .orElseGet(() -> {
                    final Conversation conversation = new Conversation(null, ModeConversation.IA);
                    conversation.setTelephone(telephone);
                    conversation.setClient(client);
                    return conversationRepository.save(conversation);
                });
    }

    private void basculerHumain(final Conversation conversation) {
        conversation.setMode(ModeConversation.HUMAIN);
        conversationRepository.save(conversation);
    }

    private WhatsAppMessage messageSortant(final String telephone, final String wamid,
                                           final String contenu, final TypeMessage type,
                                           final String templateNom) {
        return new WhatsAppMessage(wamid, DirectionMessage.SORTANT, telephone, type, contenu,
                StatutMessage.ENVOYE, null, templateNom);
    }
}
