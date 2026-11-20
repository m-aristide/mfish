package com.mfish.erp.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mfish.erp.whatsapp.webhook.DirectionMessage;
import com.mfish.erp.whatsapp.webhook.StatutMessage;
import com.mfish.erp.whatsapp.webhook.TypeMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessage;
import com.mfish.erp.whatsapp.webhook.WhatsAppMessageRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class HistoriqueConversationTest {

    private static final String TELEPHONE = "+237690445566";

    @Mock
    private WhatsAppMessageRepository messageRepository;

    private HistoriqueConversation historique() {
        return new HistoriqueConversation(messageRepository);
    }

    private static WhatsAppMessage message(final String wamid, final DirectionMessage direction,
                                           final String contenu) {
        return new WhatsAppMessage(wamid, direction, TELEPHONE, TypeMessage.TEXTE, contenu,
                StatutMessage.RECU, null, null);
    }

    @Test
    @DisplayName("doit_remettre_l_historique_dans_l_ordre_chronologique_avec_les_locuteurs")
    void doit_remettre_l_historique_dans_l_ordre_chronologique_avec_les_locuteurs() {
        when(messageRepository.findByTelephoneOrderByCreatedAtDesc(anyString(), any(Pageable.class)))
                .thenReturn(List.of(
                        message("w3", DirectionMessage.SORTANT, "Tres bien."),
                        message("w2", DirectionMessage.ENTRANT, "Obobogo"),
                        message("w1", DirectionMessage.ENTRANT, "3 kilos")));

        final List<String> historique = historique().charger(TELEPHONE, null);

        assertThat(historique).containsExactly("Client : 3 kilos", "Client : Obobogo",
                "Assistant : Tres bien.");
    }

    @Test
    @DisplayName("doit_exclure_le_message_en_cours_et_les_messages_vides")
    void doit_exclure_le_message_en_cours_et_les_messages_vides() {
        when(messageRepository.findByTelephoneOrderByCreatedAtDesc(anyString(), any(Pageable.class)))
                .thenReturn(List.of(
                        message("w4", DirectionMessage.ENTRANT, "le 20/11/2026"),
                        message("w3", DirectionMessage.ENTRANT, "   "),
                        message("w2", DirectionMessage.SORTANT, null),
                        message("w1", DirectionMessage.ENTRANT, "3 kilos")));

        final List<String> historique = historique().charger(TELEPHONE, "w4");

        assertThat(historique).containsExactly("Client : 3 kilos");
    }

    @Test
    @DisplayName("doit_limiter_le_nombre_de_messages_relus")
    void doit_limiter_le_nombre_de_messages_relus() {
        when(messageRepository.findByTelephoneOrderByCreatedAtDesc(anyString(), any(Pageable.class)))
                .thenReturn(List.of());

        historique().charger(TELEPHONE, null);

        final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository)
                .findByTelephoneOrderByCreatedAtDesc(anyString(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(12);
    }

    @Test
    @DisplayName("ne_doit_pas_interroger_la_base_sans_numero")
    void ne_doit_pas_interroger_la_base_sans_numero() {
        assertThat(historique().charger(null, null)).isEmpty();
        assertThat(historique().charger(" ", null)).isEmpty();
        verify(messageRepository, never())
                .findByTelephoneOrderByCreatedAtDesc(anyString(), any(Pageable.class));
    }
}
