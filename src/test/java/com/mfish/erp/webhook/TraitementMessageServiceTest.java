package com.mfish.erp.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.conversation.Conversation;
import com.mfish.erp.conversation.ConversationRepository;
import com.mfish.erp.conversation.EtatCommande;
import com.mfish.erp.conversation.HistoriqueConversation;
import com.mfish.erp.conversation.LigneEtatCommande;
import com.mfish.erp.conversation.MessagingPort;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.conversation.RegistreMessagesEmis;
import com.mfish.erp.ia.OrchestrateurIa;
import com.mfish.erp.ia.ResultatIa;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TraitementMessageServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private OrchestrateurIa orchestrateurIa;

    @Mock
    private MessagingPort messagingPort;

    @Mock
    private WhatsAppMessageRepository messageRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneId.of("UTC"));

    private final RegistreMessagesEmis registre = new RegistreMessagesEmis(clock);

    private TraitementMessageService service() {
        return new TraitementMessageService(conversationRepository, clientRepository,
                orchestrateurIa, messagingPort, registre,
                new HistoriqueConversation(messageRepository), messageRepository, clock);
    }

    private WebhookPayload entrant() {
        return new WebhookPayload("message_created", "evt-1", "incoming", false, "2 attieke",
                "conv-1", "contact", "+2250700000001", "Awa");
    }

    @Test
    @DisplayName("doit_ignorer_une_note_privee")
    void doit_ignorer_une_note_privee() {
        service().traiter(new WebhookPayload("message_created", "evt-1", "incoming", true, "note",
                "conv-1", "contact", "+2250700000001", "Awa"));

        verifyNoInteractions(orchestrateurIa, messagingPort, conversationRepository);
    }

    @Test
    @DisplayName("doit_basculer_en_humain_sur_reponse_humaine")
    void doit_basculer_en_humain_sur_reponse_humaine() {
        when(conversationRepository.findByChatwootConversationId("conv-1")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        service().traiter(new WebhookPayload("message_created", "evt-1", "outgoing", false,
                "je m en occupe", "conv-1", "user", null, null));

        final ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getMode()).isEqualTo(ModeConversation.HUMAIN);
        verifyNoInteractions(orchestrateurIa, messagingPort);
    }

    @Test
    @DisplayName("doit_ignorer_l_echo_de_la_reponse_ia_sans_basculer_en_humain")
    void doit_ignorer_l_echo_de_la_reponse_ia_sans_basculer_en_humain() {
        registre.enregistrer("conv-1", "Oui, nous avons du poisson silure frais.");

        service().traiter(new WebhookPayload("message_created", "evt-2", "outgoing", false,
                "Oui,  nous avons du poisson silure frais.\n", "conv-1", "user", null, null));

        verifyNoInteractions(conversationRepository, orchestrateurIa, messagingPort);
    }

    @Test
    @DisplayName("doit_rester_silencieux_en_mode_humain")
    void doit_rester_silencieux_en_mode_humain() {
        when(conversationRepository.findByChatwootConversationId("conv-1"))
                .thenReturn(Optional.of(new Conversation("conv-1", ModeConversation.HUMAIN)));

        service().traiter(entrant());

        verifyNoInteractions(orchestrateurIa, messagingPort);
    }

    @Test
    @DisplayName("doit_repondre_via_ia_en_mode_ia")
    void doit_repondre_via_ia_en_mode_ia() {
        when(conversationRepository.findByChatwootConversationId("conv-1")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findByTelephone("+2250700000001"))
                .thenReturn(Optional.of(new Client("Awa", "+2250700000001")));
        when(orchestrateurIa.traiter(any(), any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false));

        service().traiter(entrant());

        verify(messagingPort).envoyerMessage("conv-1", "ok");
        verify(messagingPort, never()).etiqueterConversation(anyString(), anyString());
    }

    @Test
    @DisplayName("doit_escalader_et_etiqueter_si_l_ia_demande_escalade")
    void doit_escalader_et_etiqueter_si_l_ia_demande_escalade() {
        when(conversationRepository.findByChatwootConversationId("conv-1")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findByTelephone("+2250700000001"))
                .thenReturn(Optional.of(new Client("Awa", "+2250700000001")));
        when(orchestrateurIa.traiter(any(), any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("reclamation", true, false));

        service().traiter(entrant());

        verify(messagingPort).envoyerMessage("conv-1", "reclamation");
        verify(messagingPort).etiqueterConversation("conv-1", "reprendre-main");
    }

    @Test
    @DisplayName("ne_duplique_pas_le_message_courant_dans_l_historique_ia")
    @SuppressWarnings("unchecked")
    void ne_duplique_pas_le_message_courant_dans_l_historique_ia() {
        when(conversationRepository.findByChatwootConversationId("conv-1")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findByTelephone("+2250700000001"))
                .thenReturn(Optional.of(new Client("Awa", "+2250700000001")));
        when(orchestrateurIa.traiter(any(), any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false));

        service().traiter(entrant());

        final ArgumentCaptor<List<String>> historique = ArgumentCaptor.forClass((Class) List.class);
        final ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(orchestrateurIa).traiter(any(), any(), historique.capture(), message.capture());
        assertThat(historique.getValue()).isEmpty();
        assertThat(message.getValue()).isEqualTo("2 attieke");
    }

    @Test
    @DisplayName("doit_transmettre_l_etat_de_commande_persiste_a_l_orchestrateur")
    void doit_transmettre_l_etat_de_commande_persiste_a_l_orchestrateur() {
        final Conversation existante = new Conversation("conv-1", ModeConversation.IA);
        existante.setEtatCommandeJson("{\"lignes\":[{\"nom\":\"Attieke\",\"quantite\":3}],"
                + "\"quartier\":\"Cocody\",\"statut\":\"EN_COURS\"}");
        when(conversationRepository.findByChatwootConversationId("conv-1"))
                .thenReturn(Optional.of(existante));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findByTelephone("+2250700000001"))
                .thenReturn(Optional.of(new Client("Awa", "+2250700000001")));
        when(orchestrateurIa.traiter(any(), any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false));

        service().traiter(entrant());

        final ArgumentCaptor<EtatCommande> etat = ArgumentCaptor.forClass(EtatCommande.class);
        final ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(orchestrateurIa).traiter(any(), etat.capture(), anyList(), message.capture());
        assertThat(etat.getValue().lignes()).hasSize(1);
        assertThat(etat.getValue().lignes().getFirst().quantite()).isEqualTo(3);
        assertThat(etat.getValue().quartier()).isEqualTo("Cocody");
        assertThat(message.getValue()).isEqualTo("2 attieke");
    }

    @Test
    @DisplayName("doit_persister_l_etat_de_commande_retourne_par_l_ia")
    void doit_persister_l_etat_de_commande_retourne_par_l_ia() {
        final Conversation existante = new Conversation("conv-1", ModeConversation.IA);
        when(conversationRepository.findByChatwootConversationId("conv-1"))
                .thenReturn(Optional.of(existante));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findByTelephone("+2250700000001"))
                .thenReturn(Optional.of(new Client("Awa", "+2250700000001")));
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 2)), null, null, null, null, null);
        when(orchestrateurIa.traiter(any(), any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false, etat));

        service().traiter(entrant());

        final ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getEtatCommandeJson())
                .contains("\"nom\":\"Attieke\"")
                .contains("\"quantite\":2");
    }

    @Test
    @DisplayName("doit_persister_les_messages_entrant_et_sortant_pour_l_historique")
    void doit_persister_les_messages_entrant_et_sortant_pour_l_historique() {
        when(conversationRepository.findByChatwootConversationId("conv-1")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findByTelephone("+2250700000001"))
                .thenReturn(Optional.of(new Client("Awa", "+2250700000001")));
        when(orchestrateurIa.traiter(any(), any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false));

        service().traiter(entrant());

        final ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
        verify(messageRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().stream().map(WhatsAppMessage::getWamid).toList())
                .anyMatch("chatwoot-evt-1"::equals)
                .anyMatch(wamid -> wamid.startsWith("chatwoot-out-"));
    }

    @Test
    @DisplayName("ne_duplique_pas_le_message_entrant_deja_persiste")
    void ne_duplique_pas_le_message_entrant_deja_persiste() {
        when(conversationRepository.findByChatwootConversationId("conv-1")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findByTelephone("+2250700000001"))
                .thenReturn(Optional.of(new Client("Awa", "+2250700000001")));
        when(messageRepository.existsByWamid("chatwoot-evt-1")).thenReturn(true);
        when(orchestrateurIa.traiter(any(), any(), anyList(), anyString()))
                .thenReturn(new ResultatIa("ok", false, false));

        service().traiter(entrant());

        final ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
        verify(messageRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getWamid()).startsWith("chatwoot-out-");
    }
}

