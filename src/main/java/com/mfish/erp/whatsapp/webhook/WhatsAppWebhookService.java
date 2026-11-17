package com.mfish.erp.whatsapp.webhook;

import com.mfish.erp.whatsapp.metrics.WhatsAppMetrics;
import com.mfish.erp.whatsapp.service.WhatsAppConversationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-traitement synchrone des webhooks WhatsApp : idempotence (wamid unique), mise a jour
 * des statuts, limitation de debit, puis traitement metier asynchrone.
 */
@Service
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppWebhookService {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsAppWebhookService.class);

    private final WhatsAppMessageRepository messageRepository;
    private final WhatsAppConversationService conversationService;
    private final TaskExecutor taskExecutor;
    private final RateLimiterWhatsApp rateLimiter;
    private final WhatsAppMetrics metrics;

    public WhatsAppWebhookService(final WhatsAppMessageRepository messageRepository,
                                  final WhatsAppConversationService conversationService,
                                  final TaskExecutor taskExecutor,
                                  final RateLimiterWhatsApp rateLimiter,
                                  final WhatsAppMetrics metrics) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.taskExecutor = taskExecutor;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
    }

    @Transactional
    public void traiter(final List<MessageEntrant> messages, final List<StatutEntrant> statuts) {
        if (statuts != null) {
            statuts.forEach(this::traiterStatut);
        }
        if (messages != null) {
            messages.forEach(this::traiterMessage);
        }
    }

    private void traiterStatut(final StatutEntrant statut) {
        if (statut.wamid() == null || statut.wamid().isBlank()) {
            return;
        }
        messageRepository.findByWamid(statut.wamid()).ifPresent(message -> {
            message.setStatut(mapStatut(statut.statut()));
            if ("failed".equals(statut.statut())) {
                LOG.warn("Echec d'envoi WhatsApp, code={}", statut.codeErreur());
                metrics.incrementerEchecs();
            }
            messageRepository.save(message);
        });
    }

    private void traiterMessage(final MessageEntrant message) {
        if (message.wamid() == null || message.wamid().isBlank()) {
            return;
        }
        if (messageRepository.existsByWamid(message.wamid())) {
            metrics.incrementerDoublons();
            return;
        }
        if (!rateLimiter.autorise(message.telephone())) {
            metrics.incrementerRateLimites();
            return;
        }
        messageRepository.save(new WhatsAppMessage(message.wamid(), DirectionMessage.ENTRANT,
                message.telephone(), message.type(), message.texte(), StatutMessage.RECU,
                message.mediaId(), null));
        metrics.incrementerEntrants();
        taskExecutor.execute(() -> conversationService.traiter(message));
    }

    private StatutMessage mapStatut(final String statut) {
        return switch (statut == null ? "" : statut) {
            case "sent" -> StatutMessage.ENVOYE;
            case "delivered" -> StatutMessage.LIVRE;
            case "read" -> StatutMessage.LU;
            case "failed" -> StatutMessage.ECHEC;
            default -> StatutMessage.ENVOYE;
        };
    }
}
