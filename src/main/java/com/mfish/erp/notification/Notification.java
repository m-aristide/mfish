package com.mfish.erp.notification;

import com.mfish.erp.commande.Commande;
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
import org.hibernate.annotations.CreationTimestamp;

/**
 * Notification envoyee (ou en echec) a un client, pour une commande donnee.
 * La contrainte unique (commande, type) garantit l'absence de doublon.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;

    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutNotification statut;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(final Commande commande, final TypeNotification type,
                        final String contenu, final StatutNotification statut) {
        this.commande = commande;
        this.type = type;
        this.contenu = contenu;
        this.statut = statut;
    }

    public UUID getId() {
        return id;
    }

    public Commande getCommande() {
        return commande;
    }

    public TypeNotification getType() {
        return type;
    }

    public String getContenu() {
        return contenu;
    }

    public StatutNotification getStatut() {
        return statut;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
