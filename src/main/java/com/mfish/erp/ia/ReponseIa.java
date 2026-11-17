package com.mfish.erp.ia;

/**
 * Reponse stricte attendue de l'IA, mappee sur un record puis validee.
 */
public record ReponseIa(
        String reponse,
        String intention,
        CommandeIa commande,
        boolean escalade) {

    public Intention intentionEnum() {
        return Intention.fromString(intention);
    }
}
