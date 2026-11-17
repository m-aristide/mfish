package com.mfish.erp.parametre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Parametre applicatif (cle/valeur), par exemple duree d'arret, vitesse moyenne.
 */
@Entity
@Table(name = "parametres")
public class Parametre {

    @Id
    private String cle;

    private String valeur;

    protected Parametre() {
    }

    public Parametre(String cle, String valeur) {
        this.cle = cle;
        this.valeur = valeur;
    }

    public String getCle() {
        return cle;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }
}
