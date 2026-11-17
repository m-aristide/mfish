package com.mfish.erp.commande;

/**
 * Levee lorsqu'une transition de statut de commande est invalide.
 */
public class TransitionStatutInvalideException extends RuntimeException {

    public TransitionStatutInvalideException(StatutCommande de, StatutCommande vers) {
        super("Transition de statut invalide : " + de + " -> " + vers);
    }
}
