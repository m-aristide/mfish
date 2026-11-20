package com.mfish.erp.conversation;

/**
 * Ligne d'une commande en cours de construction : nom libre donne par le client et quantite
 * en kilos. Le prix reste lu en base au moment de l'enregistrement.
 */
public record LigneEtatCommande(String nom, int quantite) {

    public LigneEtatCommande {
        nom = nom == null ? "" : nom.trim();
    }
}
