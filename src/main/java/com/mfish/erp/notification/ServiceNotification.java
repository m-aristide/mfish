package com.mfish.erp.notification;

import com.mfish.erp.commande.Commande;
import com.mfish.erp.conversation.MessagingPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Envoi de notifications clients : heure de livraison et confirmation de livraison.
 * Anti-doublon en base, regle des 24 h WhatsApp, statut d'envoi suivi.
 */
@Service
public class ServiceNotification {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceNotification.class);
    private static final String TEMPLATE_HORS_FENETRE =
            "Bonjour, votre commande est en cours de livraison. Merci.";

    private final NotificationRepository notificationRepository;
    private final MessagingPort messagingPort;
    private final Clock clock;

    public ServiceNotification(final NotificationRepository notificationRepository,
                               final MessagingPort messagingPort,
                               final Clock clock) {
        this.notificationRepository = notificationRepository;
        this.messagingPort = messagingPort;
        this.clock = clock;
    }

    /**
     * Notifie l'heure de livraison estimee. Retourne {@code false} si deja envoyee ou en echec.
     */
    public boolean notifierHeureLivraison(final Commande commande, final String conversationId,
                                          final LocalTime heureEstimee, final Instant derniereActivite) {
        return notifier(commande, conversationId, TypeNotification.HEURE_LIVRAISON,
                FormatteurFenetre.formater(heureEstimee), derniereActivite);
    }

    /**
     * Notifie la confirmation de livraison. Retourne {@code false} si deja envoyee ou en echec.
     */
    public boolean notifierConfirmationLivraison(final Commande commande, final String conversationId,
                                                 final Instant derniereActivite) {
        return notifier(commande, conversationId, TypeNotification.CONFIRMATION_LIVRAISON,
                "Votre commande a ete livree. Merci.", derniereActivite);
    }

    private boolean notifier(final Commande commande, final String conversationId,
                             final TypeNotification type, final String contenuLibre,
                             final Instant derniereActivite) {
        if (notificationRepository.existsByCommandeIdAndType(commande.getId(), type)) {
            return false;
        }
        final String contenu = Regle24Heures.peutEnvoyerMessageLibre(derniereActivite, clock)
                ? contenuLibre
                : TEMPLATE_HORS_FENETRE;
        try {
            messagingPort.envoyerMessage(conversationId, contenu);
            notificationRepository.save(new Notification(commande, type, contenu, StatutNotification.ENVOYEE));
            return true;
        } catch (final Exception e) {
            LOG.warn("Echec d'envoi de la notification {} pour la commande {}", type, commande.getId(), e);
            notificationRepository.save(new Notification(commande, type, contenu, StatutNotification.ECHEC));
            return false;
        }
    }
}
