package com.mfish.erp.tournee;

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
import java.time.LocalTime;
import java.util.UUID;

/**
 * Arret d'une tournee, rattache a une commande.
 */
@Entity
@Table(name = "arrets_tournee")
public class ArretTournee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournee_id", nullable = false)
    private Tournee tournee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @Column(nullable = false)
    private int position;

    @Column(name = "heure_estimee")
    private LocalTime heureEstimee;

    @Column(name = "heure_reelle")
    private LocalTime heureReelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutArret statut;

    protected ArretTournee() {
    }

    public ArretTournee(Tournee tournee, Commande commande, int position, StatutArret statut) {
        this.tournee = tournee;
        this.commande = commande;
        this.position = position;
        this.statut = statut;
    }

    public UUID getId() {
        return id;
    }

    public Tournee getTournee() {
        return tournee;
    }

    public Commande getCommande() {
        return commande;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public LocalTime getHeureEstimee() {
        return heureEstimee;
    }

    public void setHeureEstimee(LocalTime heureEstimee) {
        this.heureEstimee = heureEstimee;
    }

    public LocalTime getHeureReelle() {
        return heureReelle;
    }

    public void setHeureReelle(LocalTime heureReelle) {
        this.heureReelle = heureReelle;
    }

    public StatutArret getStatut() {
        return statut;
    }

    public void setStatut(StatutArret statut) {
        this.statut = statut;
    }
}
