package com.mfish.erp.ia;

/**
 * Intention detectee par l'IA dans un message client.
 */
public enum Intention {
    COMMANDE,
    QUESTION,
    MODIFICATION,
    ANNULATION,
    AUTRE;

    /**
     * Conversion tolerante (minuscules) de la valeur JSON.
     */
    public static Intention fromString(final String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return AUTRE;
        }
        return switch (valeur.trim().toLowerCase()) {
            case "commande" -> COMMANDE;
            case "question" -> QUESTION;
            case "modification" -> MODIFICATION;
            case "annulation" -> ANNULATION;
            default -> AUTRE;
        };
    }
}
