package com.mfish.erp.conversation;

import com.mfish.erp.whatsapp.webhook.DirectionMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessageRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconstruit l'historique lisible d'une conversation a partir des messages persistes.
 *
 * <p>Les messages sont lus du plus recent au plus ancien (borne basse pour ne pas saturer le
 * contexte du LLM) puis remis dans l'ordre chronologique. Le message en cours de traitement est
 * exclu : il est ajoute explicitement par l'orchestrateur, sinon il serait envoye deux fois.
 */
@Component
public class HistoriqueConversation {

    private static final int MAX_MESSAGES = 12;

    private final WhatsAppMessageRepository messageRepository;

    public HistoriqueConversation(final WhatsAppMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Historique chronologique d'un numero, au format « Client : ... » / « Assistant : ... ».
     *
     * @param telephone  numero E.164 de la conversation
     * @param wamidExclu identifiant du message en cours, ignore s'il est deja persiste
     */
    @Transactional(readOnly = true)
    public List<String> charger(final String telephone, final String wamidExclu) {
        if (telephone == null || telephone.isBlank()) {
            return List.of();
        }
        final List<WhatsAppMessage> recents = messageRepository
                .findByTelephoneOrderByCreatedAtDesc(telephone, PageRequest.of(0, MAX_MESSAGES));
        final List<String> historique = new ArrayList<>();
        for (final WhatsAppMessage message : recents) {
            if (message.getContenu() == null || message.getContenu().isBlank()) {
                continue;
            }
            if (wamidExclu != null && wamidExclu.equals(message.getWamid())) {
                continue;
            }
            historique.add(prefixe(message.getDirection()) + message.getContenu().trim());
        }
        Collections.reverse(historique);
        return historique;
    }

    private static String prefixe(final DirectionMessage direction) {
        return direction == DirectionMessage.ENTRANT ? "Client : " : "Assistant : ";
    }
}
