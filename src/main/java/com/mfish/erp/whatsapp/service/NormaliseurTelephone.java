package com.mfish.erp.whatsapp.service;

import java.util.regex.Pattern;

/**
 * Normalisation d'un numero de telephone au format E.164 (+CC...).
 * Retourne {@code null} si le numero ne peut pas etre normalise.
 */
public final class NormaliseurTelephone {

    private static final Pattern E164 = Pattern.compile("\\+[1-9][0-9]{6,14}");

    private NormaliseurTelephone() {
    }

    public static String normaliser(final String brut) {
        if (brut == null || brut.isBlank()) {
            return null;
        }
        String valeur = brut.trim();
        if (valeur.startsWith("00")) {
            valeur = "+" + valeur.substring(2);
        }
        valeur = valeur.replaceAll("[\\s\\-().]", "");
        if (!valeur.startsWith("+")) {
            valeur = "+" + valeur;
        }
        return E164.matcher(valeur).matches() ? valeur : null;
    }
}
