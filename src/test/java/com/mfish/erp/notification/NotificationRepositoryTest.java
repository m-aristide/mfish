package com.mfish.erp.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mfish.erp.TestcontainersConfiguration;
import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.Commande;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commande.StatutPaiement;
import com.mfish.erp.config.ApplicationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ApplicationConfig.class})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("doit_rejeter_un_doublon_pour_la_meme_commande_et_le_meme_type")
    void doit_rejeter_un_doublon_pour_la_meme_commande_et_le_meme_type() {
        final Client client = clientRepository.saveAndFlush(new Client("Awa", "+2250799999001"));
        final Commande commande = commandeRepository.saveAndFlush(new Commande(client,
                StatutCommande.A_PLANIFIER, ModePaiement.ESPECES, StatutPaiement.NON_PAYE,
                SourceCommande.MANUEL));

        notificationRepository.saveAndFlush(new Notification(commande, TypeNotification.HEURE_LIVRAISON,
                "contenu", StatutNotification.ENVOYEE));

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(new Notification(commande,
                TypeNotification.HEURE_LIVRAISON, "contenu2", StatutNotification.ENVOYEE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
