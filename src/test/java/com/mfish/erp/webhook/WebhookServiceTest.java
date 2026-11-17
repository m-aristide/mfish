package com.mfish.erp.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private EvenementTraiteRepository evenementTraiteRepository;

    @Mock
    private TraitementMessageService traitementMessageService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneId.of("UTC"));

    private WebhookService service() {
        return new WebhookService(evenementTraiteRepository, traitementMessageService,
                task -> task.run(), clock);
    }

    private WebhookPayload message() {
        return new WebhookPayload("message_created", "evt-1", "incoming", false, "bonjour",
                "conv-1", "contact", "+2250700000001", "Awa");
    }

    @Test
    @DisplayName("doit_ignorer_un_evenement_deja_traite")
    void doit_ignorer_un_evenement_deja_traite() {
        when(evenementTraiteRepository.existsById("evt-1")).thenReturn(true);

        service().traiter(message());

        verify(evenementTraiteRepository, never()).save(any());
        verifyNoInteractions(traitementMessageService);
    }

    @Test
    @DisplayName("doit_ignorer_un_evenement_non_message")
    void doit_ignorer_un_evenement_non_message() {
        final WebhookPayload payload = new WebhookPayload("conversation_created", "evt-1",
                "incoming", false, null, "conv-1", "contact", null, null);

        service().traiter(payload);

        verifyNoInteractions(evenementTraiteRepository, traitementMessageService);
    }

    @Test
    @DisplayName("doit_marquer_et_traiter_un_nouveau_message")
    void doit_marquer_et_traiter_un_nouveau_message() {
        when(evenementTraiteRepository.existsById("evt-1")).thenReturn(false);

        service().traiter(message());

        verify(evenementTraiteRepository).save(any());
        verify(traitementMessageService).traiter(any());
    }
}
