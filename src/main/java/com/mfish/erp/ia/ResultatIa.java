package com.mfish.erp.ia;

import com.mfish.erp.conversation.EtatCommande;

/**
 * Resultat d'un traitement IA : reponse au client, besoin d'escalade, commande creee et etat
 * structure de la commande (a repersister dans la conversation).
 */
public record ResultatIa(String reponseAuClient, boolean escalade, boolean commandeCreee,
                         EtatCommande etat) {

    public ResultatIa {
        etat = etat == null ? EtatCommande.vide() : etat;
    }

    /** Resultat sans etat de commande connu (traitements hors conversation WhatsApp). */
    public ResultatIa(final String reponseAuClient, final boolean escalade,
                      final boolean commandeCreee) {
        this(reponseAuClient, escalade, commandeCreee, EtatCommande.vide());
    }
}
