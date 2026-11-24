package com.mfish.erp.webhook;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-traitement synchrone du webhook : idempotence (table evenements_traites), filtrage des
 * evenements non-message, puis declenchement du traitement asynchrone.
 */
@Service
public class WebhookService {

    private final EvenementTraiteRepository evenementTraiteRepository;
    private final TraitementMessageService traitementMessageService;
    private final TaskExecutor taskExecutor;
    private final Clock clock;

    public WebhookService(final EvenementTraiteRepository evenementTraiteRepository,
                          final TraitementMessageService traitementMessageService,
                          @Qualifier("applicationTaskExecutor") final TaskExecutor taskExecutor,
                          final Clock clock) {
        this.evenementTraiteRepository = evenementTraiteRepository;
        this.traitementMessageService = traitementMessageService;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    @Transactional
    public void traiter(final WebhookPayload payload) {
        if (!"message_created".equals(payload.event())) {
            return;
        }
        if (payload.id() == null || payload.id().isBlank()) {
            return;
        }
        if (evenementTraiteRepository.existsById(payload.id())) {
            return;
        }
        evenementTraiteRepository.save(new EvenementTraite(payload.id(), clock.instant()));

        taskExecutor.execute(() -> traitementMessageService.traiter(payload));
    }
}
