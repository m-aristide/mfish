package com.mfish.erp.connaissance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Version de la base de connaissance de l'IA (contenu Markdown). Chaque enregistrement
 * cree une nouvelle version (historique immuable) ; la version courante est la plus recente.
 */
@Entity
@Table(name = "connaissances")
public class Connaissance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 1_000_000)
    private String contenu;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Connaissance() {
    }

    public Connaissance(final String contenu) {
        this.contenu = contenu;
    }

    public UUID getId() {
        return id;
    }

    public String getContenu() {
        return contenu;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
