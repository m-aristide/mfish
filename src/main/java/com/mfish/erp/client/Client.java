package com.mfish.erp.client;

import com.mfish.erp.commun.AbstractAuditable;
import com.mfish.erp.quartier.Quartier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Client livre par le commercant.
 */
@Entity
@Table(name = "clients")
public class Client extends AbstractAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String telephone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quartier_id")
    private Quartier quartier;

    private String adresse;

    private String repere;

    private Double latitude;

    private Double longitude;

    private String notes;

    @Column(nullable = false)
    private boolean archive = false;

    protected Client() {
    }

    public Client(String nom, String telephone) {
        this.nom = nom;
        this.telephone = telephone;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Quartier getQuartier() {
        return quartier;
    }

    public void setQuartier(Quartier quartier) {
        this.quartier = quartier;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getRepere() {
        return repere;
    }

    public void setRepere(String repere) {
        this.repere = repere;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isArchive() {
        return archive;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }
}
