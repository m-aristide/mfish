package com.mfish.erp.whatsapp.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Message WhatsApp stocke localement : historique, idempotence (wamid unique)
 * et suivi des statuts de livraison.
 */
@Entity
@Table(name = "whatsapp_messages")
public class WhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wamid", unique = true)
    private String wamid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectionMessage direction;

    @Column(nullable = false)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMessage type;

    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMessage statut;

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "template_nom")
    private String templateNom;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WhatsAppMessage() {
    }

    public WhatsAppMessage(final String wamid, final DirectionMessage direction,
                           final String telephone, final TypeMessage type,
                           final String contenu, final StatutMessage statut,
                           final String mediaId, final String templateNom) {
        this.wamid = wamid;
        this.direction = direction;
        this.telephone = telephone;
        this.type = type;
        this.contenu = contenu;
        this.statut = statut;
        this.mediaId = mediaId;
        this.templateNom = templateNom;
    }

    public UUID getId() {
        return id;
    }

    public String getWamid() {
        return wamid;
    }

    public void setWamid(String wamid) {
        this.wamid = wamid;
    }

    public DirectionMessage getDirection() {
        return direction;
    }

    public String getTelephone() {
        return telephone;
    }

    public TypeMessage getType() {
        return type;
    }

    public String getContenu() {
        return contenu;
    }

    public StatutMessage getStatut() {
        return statut;
    }

    public void setStatut(StatutMessage statut) {
        this.statut = statut;
    }

    public String getMediaId() {
        return mediaId;
    }

    public String getTemplateNom() {
        return templateNom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
