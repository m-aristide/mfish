package com.mfish.erp.ia;

import com.mfish.erp.quartier.Quartier;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Rapprochement d'un nom de quartier (donne par l'IA) vers la liste des quartiers de la
 * base. Retourne le quartier le plus probable ; si plusieurs correspondent, signale
 * l'ambiguite pour que la commande soit annotee.
 */
@Component
public class RapprochementQuartiers {

    /**
     * @param quartier quartier retenu (le premier en cas d'ambiguite)
     * @param ambigu   vrai si plusieurs quartiers correspondent
     */
    public record QuartierRapproche(Quartier quartier, boolean ambigu) {
    }

    /**
     * Rapproche un nom vers les quartiers actifs. Retourne vide si le nom est absent ou
     * si aucun quartier ne correspond.
     */
    public Optional<QuartierRapproche> rapprocher(final String nom, final List<Quartier> quartiers) {
        if (nom == null || nom.isBlank()) {
            return Optional.empty();
        }
        final String cible = SimilariteTexte.normaliser(nom);
        final List<Quartier> actifs = quartiers.stream().filter(Quartier::isActif).toList();

        final List<Quartier> exacts = actifs.stream()
                .filter(quartier -> SimilariteTexte.normaliser(quartier.getNom()).equals(cible))
                .toList();
        if (!exacts.isEmpty()) {
            return Optional.of(new QuartierRapproche(exacts.get(0), exacts.size() > 1));
        }

        final List<Quartier> proches = actifs.stream()
                .filter(quartier -> SimilariteTexte.levenshtein(
                        SimilariteTexte.normaliser(quartier.getNom()), cible) <= 1)
                .toList();
        if (!proches.isEmpty()) {
            return Optional.of(new QuartierRapproche(proches.get(0), proches.size() > 1));
        }
        return Optional.empty();
    }
}