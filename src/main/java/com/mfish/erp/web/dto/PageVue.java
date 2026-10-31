package com.mfish.erp.web.dto;

import java.util.List;

/**
 * Page de resultats (pagination cote serveur).
 */
public record PageVue<T>(List<T> elements, int page, int taille, long totalElements, int totalPages) {

    public boolean hasPrecedent() {
        return page > 0;
    }

    public boolean hasSuivant() {
        return page + 1 < totalPages;
    }
}
