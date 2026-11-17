package com.mfish.erp.tournee;

import java.util.ArrayList;
import java.util.List;

/**
 * Decoupage d'une liste en lots de taille maximale (limite d'arrets d'un fournisseur de routage).
 */
public final class DecoupageLots {

    private DecoupageLots() {
    }

    public static <T> List<List<T>> decouper(final List<T> elements, final int tailleMax) {
        if (tailleMax <= 0) {
            throw new IllegalArgumentException("La taille maximale doit etre strictement positive");
        }
        final List<List<T>> lots = new ArrayList<>();
        for (int i = 0; i < elements.size(); i += tailleMax) {
            lots.add(new ArrayList<>(elements.subList(i, Math.min(i + tailleMax, elements.size()))));
        }
        return lots;
    }
}
