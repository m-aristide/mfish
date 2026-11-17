package com.mfish.erp.whatsapp.service;

import java.text.Normalizer;
import java.util.List;

/**
 * Detection des mots-cles d'opposition (STOP, ARRET, desinscription, ...).
 */
public final class DetecteurOptOut {

    private static final List<String> MOTS_CLES = List.of(
            "stop", "arret", "desinscrire", "desinscription", "desabonner", "desabonnement",
            "opt-out", "optout", "ne plus me contacter", "ne plus m ecrire", "retire moi");

    private DetecteurOptOut() {
    }

    public static boolean estOpposition(final String texte) {
        if (texte == null || texte.isBlank()) {
            return false;
        }
        final String normalise = normaliser(texte);
        return MOTS_CLES.stream().anyMatch(normalise::contains);
    }

    private static String normaliser(final String texte) {
        final String minuscules = texte.toLowerCase();
        return Normalizer.normalize(minuscules, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
