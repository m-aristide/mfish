package com.mfish.erp.commun;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Champs d'audit (created_at / updated_at) partages par les entites qui en ont besoin.
 * Les valeurs sont alimentees par Hibernate au moment du flush (metadonnees d'audit) ;
 * la logique metier, elle, utilise le {@code Clock} injecte.
 */
@MappedSuperclass
public abstract class AbstractAuditable {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AbstractAuditable() {
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
