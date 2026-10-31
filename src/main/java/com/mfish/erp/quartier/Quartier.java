package com.mfish.erp.quartier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Quartier de livraison.
 */
@Entity
@Table(name = "quartiers")
public class Quartier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nom;

    private String zone;

    private Double latitude;

    private Double longitude;

    @Column(name = "ordre_defaut", nullable = false)
    private int ordreDefaut;

    @Column(nullable = false)
    private boolean actif = true;

    protected Quartier() {
    }

    public Quartier(String nom, String zone, Double latitude, Double longitude, int ordreDefaut, boolean actif) {
        this.nom = nom;
        this.zone = zone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.ordreDefaut = ordreDefaut;
        this.actif = actif;
    }

    public UUID getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public int getOrdreDefaut() {
        return ordreDefaut;
    }

    public void setOrdreDefaut(int ordreDefaut) {
        this.ordreDefaut = ordreDefaut;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
