package com.mfish.erp.conversation;

import com.mfish.erp.commun.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des conversations WhatsApp : liste et bascule entre le mode IA et le mode Humain.
 * L'historique des messages reste dans Chatwoot (boite partagee) ; seule la conversation
 * est suivie en base.
 */
@Service
@Transactional(readOnly = true)
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(final ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public List<ConversationDto> lister() {
        return conversationRepository.findAll().stream()
                .sorted(Comparator.comparing(Conversation::getDerniereActivite,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ConversationDto::from)
                .toList();
    }

    public ConversationDto trouver(final UUID id) {
        return conversationRepository.findById(id).map(ConversationDto::from)
                .orElseThrow(() -> new NotFoundException("Conversation introuvable : " + id));
    }

    @Transactional
    public ConversationDto basculer(final UUID id, final ModeConversation mode) {
        final Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conversation introuvable : " + id));
        conversation.setMode(mode);
        return ConversationDto.from(conversationRepository.save(conversation));
    }
}
