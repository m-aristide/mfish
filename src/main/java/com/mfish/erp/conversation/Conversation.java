package com.mfish.erp.conversation;

import com.mfish.erp.client.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Conversation WhatsApp suivie (mode IA ou HUMAIN).
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chatwoot_conversation_id", unique = true)
    private String chatwootConversationId;

    @Column(name = "telephone", unique = true)
    private String telephone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeConversation mode;

    @Column(name = "derniere_activite")
    private Instant derniereActivite;

    /** Etat structure de la commande en cours (JSON, voir {@code EtatCommandeJson}). */
    @Column(name = "order_state")
    private String etatCommandeJson;

    protected Conversation() {
    }

    public Conversation(String chatwootConversationId, ModeConversation mode) {
        this.chatwootConversationId = chatwootConversationId;
        this.mode = mode;
    }

    public UUID getId() {
        return id;
    }

    public String getChatwootConversationId() {
        return chatwootConversationId;
    }

    public void setChatwootConversationId(String chatwootConversationId) {
        this.chatwootConversationId = chatwootConversationId;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ModeConversation getMode() {
        return mode;
    }

    public void setMode(ModeConversation mode) {
        this.mode = mode;
    }

    public Instant getDerniereActivite() {
        return derniereActivite;
    }

    public void setDerniereActivite(Instant derniereActivite) {
        this.derniereActivite = derniereActivite;
    }

    public String getEtatCommandeJson() {
        return etatCommandeJson;
    }

    public void setEtatCommandeJson(String etatCommandeJson) {
        this.etatCommandeJson = etatCommandeJson;
    }
}
