package com.mfish.erp.ia;

import com.mfish.erp.produit.Produit;
import java.text.Normalizer;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Rapprochement des noms de produits donnes par l'IA vers le catalogue reel.
 * Tolerant aux fautes de frappe (Levenshtein <= 1) et au pluriel.
 */
@Component
public class RapprochementProduits {

    public record ProduitRapproche(Produit produit, int quantite) {
    }

    public List<ProduitRapproche> rapprocher(final List<ProduitIa> demandes, final List<Produit> catalogue) {
        final List<Produit> actifs = catalogue.stream().filter(Produit::isActif).toList();
        return demandes.stream()
                .map(demande -> new ProduitRapproche(trouver(demande.nom(), actifs), demande.quantite()))
                .toList();
    }

    private Produit trouver(final String nom, final List<Produit> catalogue) {
        final String cible = normaliser(nom);

        final List<Produit> exacts = catalogue.stream()
                .filter(produit -> normaliser(produit.getNom()).equals(cible))
                .toList();
        if (exacts.size() == 1) {
            return exacts.get(0);
        }
        if (exacts.size() > 1) {
            throw new ProduitAmbiguException(nom);
        }

        final List<Produit> proches = catalogue.stream()
                .filter(produit -> levenshtein(normaliser(produit.getNom()), cible) <= 1)
                .toList();
        if (proches.size() == 1) {
            return proches.get(0);
        }
        if (proches.size() > 1) {
            throw new ProduitAmbiguException(nom);
        }
        throw new ProduitInconnuException(nom);
    }

    private String normaliser(final String valeur) {
        if (valeur == null) {
            return "";
        }
        return Normalizer.normalize(valeur.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();
    }

    private int levenshtein(final String a, final String b) {
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
