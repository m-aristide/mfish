package com.mfish.erp.conversation;

/**
 * Avancement de la commande en cours de construction dans une conversation.
 *
 * <p>Ces etats representent des etapes distinctes et ne doivent jamais etre confondues :
 * informations collectees, recapitulatif presente, confirmation du client, commande enregistree
 * dans le backend, echec d'enregistrement.</p>
 */
public enum StatutEtatCommande {

    /** Informations en cours de collecte (collecting_information). */
    EN_COURS,

    /** Command complete, recapitulatif presente en attente de confirmation (ready_for_confirmation). */
    A_CONFIRMER,

    /** Client a confirme (confirmed_by_customer) : prete a etre sauvegardee. */
    CONFIRMEE,

    /** Commande enregistree dans le backend (order_saved) : ne doit plus etre modifiee par l'IA. */
    ENREGISTREE,

    /** Echec de l'enregistrement (save_failed) : transmise a l'equipe humaine. */
    ECHEC_ENREGISTREMENT
}
