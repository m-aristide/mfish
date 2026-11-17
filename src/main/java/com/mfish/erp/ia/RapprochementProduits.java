package com.mfish.erp.ia;

import com.mfish.erp.produit.Produit;
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
        final String cible = SimilariteTexte.normaliser(nom);

        final List<Produit> exacts = catalogue.stream()
                .filter(produit -> SimilariteTexte.normaliser(produit.getNom()).equals(cible))
                .toList();
        if (exacts.size() == 1) {
            return exacts.get(0);
        }
        if (exacts.size() > 1) {
            throw new ProduitAmbiguException(nom);
        }

        final List<Produit> proches = catalogue.stream()
                .filter(produit -> SimilariteTexte.levenshtein(
                        SimilariteTexte.normaliser(produit.getNom()), cible) <= 1)
                .toList();
        if (proches.size() == 1) {
            return proches.get(0);
        }
        if (proches.size() > 1) {
            throw new ProduitAmbiguException(nom);
        }
        throw new ProduitInconnuException(nom);
    }
}
