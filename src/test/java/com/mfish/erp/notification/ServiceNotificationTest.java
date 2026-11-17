package com.mfish.erp.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mfish.erp.commande.Commande;
import com.mfish.erp.conversation.MessagingPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceNotificationTest {

    private static final UUID COMMANDE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MessagingPort messagingPort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneId.of("UTC"));

    private ServiceNotification service() {
        return new ServiceNotification(notificationRepository, messagingPort, clock);
    }

    private Commande commande() {
        final Commande commande = mock(Commande.class);
        when(commande.getId()).thenReturn(COMMANDE_ID);
        return commande;
    }

    @Test
    @DisplayName("doit_formater_et_envoyer_l_heure_de_livraison")
    void doit_formater_et_envoyer_l_heure_de_livraison() {
        when(notificationRepository.existsByCommandeIdAndType(COMMANDE_ID, TypeNotification.HEURE_LIVRAISON))
                .thenReturn(false);

        final boolean resultat = service().notifierHeureLivraison(commande(), "conv-1",
                LocalTime.of(8, 30), clock.instant());

        assertThat(resultat).isTrue();
        verify(messagingPort).envoyerMessage(eq("conv-1"), contains("08:00"));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("doit_ne_pas_renvoyer_un_doublon")
    void doit_ne_pas_renvoyer_un_doublon() {
        when(notificationRepository.existsByCommandeIdAndType(COMMANDE_ID, TypeNotification.HEURE_LIVRAISON))
                .thenReturn(true);

        final boolean resultat = service().notifierHeureLivraison(commande(), "conv-1",
                LocalTime.of(8, 30), clock.instant());

        assertThat(resultat).isFalse();
        verifyNoInteractions(messagingPort);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("doit_utiliser_le_template_hors_fenetre_de_24_heures")
    void doit_utiliser_le_template_hors_fenetre_de_24_heures() {
        when(notificationRepository.existsByCommandeIdAndType(COMMANDE_ID, TypeNotification.HEURE_LIVRAISON))
                .thenReturn(false);

        service().notifierHeureLivraison(commande(), "conv-1", LocalTime.of(8, 30),
                clock.instant().minus(Duration.ofHours(25)));

        verify(messagingPort).envoyerMessage(eq("conv-1"), contains("cours de livraison"));
    }

    @Test
    @DisplayName("doit_marquer_echouee_si_l_envoi_echoue")
    void doit_marquer_echouee_si_l_envoi_echoue() {
        when(notificationRepository.existsByCommandeIdAndType(COMMANDE_ID, TypeNotification.HEURE_LIVRAISON))
                .thenReturn(false);
        doThrow(new RuntimeException("echec")).when(messagingPort)
                .envoyerMessage(anyString(), anyString());

        final boolean resultat = service().notifierHeureLivraison(commande(), "conv-1",
                LocalTime.of(8, 30), clock.instant());

        assertThat(resultat).isFalse();
        final ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutNotification.ECHEC);
    }
}
