package com.mfish.erp.whatsapp;

/**
 * Masquage des donnees personnelles (numeros de telephone) dans les journaux.
 * Aucune donnee en clair ne doit etre journalisee.
 */
public final class Masquage {

    private Masquage() {
    }

    public static String telephone(final String numero) {
        if (numero == null || numero.isBlank()) {
            return "?";
        }
        final String valeur = numero.trim();
        if (valeur.length() <= 6) {
            return "*".repeat(valeur.length());
        }
        return valeur.substring(0, 4) + " *** " + valeur.substring(valeur.length() - 2);
    }
}
