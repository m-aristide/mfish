package com.mfish.erp.ia;

import java.text.Normalizer;

/**
 * Normalisation et distance d'edition partagees par les rapprochements de texte
 * (produits, quartiers).
 */
public final class SimilariteTexte {

    private SimilariteTexte() {
    }

    /**
     * Met en minuscule, retire les accents et les espaces superflus.
     */
    public static String normaliser(final String valeur) {
        if (valeur == null) {
            return "";
        }
        return Normalizer.normalize(valeur.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();
    }

    /**
     * Distance de Levenshtein entre deux chaines.
     */
    public static int levenshtein(final String a, final String b) {
        final int[][] matrice = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            matrice[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            matrice[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                final int cout = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                matrice[i][j] = Math.min(
                        Math.min(matrice[i - 1][j] + 1, matrice[i][j - 1] + 1),
                        matrice[i - 1][j - 1] + cout);
            }
        }
        return matrice[a.length()][b.length()];
    }
}