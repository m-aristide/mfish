package com.mfish.erp.tournee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Tournee de livraison d'une journee donnee.
 */
@Entity
@Table(name = "tournees")
public class Tournee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "point_depart_lat")
    private Double pointDepartLat;

    @Column(name = "point_depart_lng")
    private Double pointDepartLng;

    @Column(name = "heure_depart")
    private LocalTime heureDepart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTournee statut;

    protected Tournee() {
    }

    public Tournee(LocalDate date, StatutTournee statut) {
        this.date = date;
        this.statut = statut;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getPointDepartLat() {
        return pointDepartLat;
    }

    public void setPointDepartLat(Double pointDepartLat) {
        this.pointDepartLat = pointDepartLat;
    }

    public Double getPointDepartLng() {
        return pointDepartLng;
    }

    public void setPointDepartLng(Double pointDepartLng) {
        this.pointDepartLng = pointDepartLng;
    }

    public LocalTime getHeureDepart() {
        return heureDepart;
    }

    public void setHeureDepart(LocalTime heureDepart) {
        this.heureDepart = heureDepart;
    }

    public StatutTournee getStatut() {
        return statut;
    }

    public void setStatut(StatutTournee statut) {
        this.statut = statut;
    }
}
