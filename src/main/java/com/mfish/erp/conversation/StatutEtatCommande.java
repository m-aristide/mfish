package com.mfish.erp.conversation;

/**
 * Avancement de la commande en cours de construction dans une conversation.
 */
public enum StatutEtatCommande {

    /** Informations en cours de collecte, commande pas encore enregistree. */
    EN_COURS,

    /** Commande enregistree : l'etat ne doit plus etre modifie par l'IA. */
    CONFIRMEE
}
