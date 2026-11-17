package com.mfish.erp.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Evenement webhook deja traite (idempotence).
 */
@Entity
@Table(name = "evenements_traites")
public class EvenementTraite {

    @Id
    @Column(name = "id_evenement")
    private String idEvenement;

    @Column(name = "traite_le", nullable = false)
    private Instant traiteLe;

    protected EvenementTraite() {
    }

    public EvenementTraite(String idEvenement, Instant traiteLe) {
        this.idEvenement = idEvenement;
        this.traiteLe = traiteLe;
    }

    public String getIdEvenement() {
        return idEvenement;
    }

    public Instant getTraiteLe() {
        return traiteLe;
    }
}
