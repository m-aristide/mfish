package com.mfish.erp.web.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtres de la liste des commandes (pagination et tri cote serveur).
 */
public record FiltreCommande(
        String statut,
        UUID quartierId,
        LocalDate date,
        String recherche,
        int page,
        int taille) {

    public static FiltreCommande defaut() {
        return new FiltreCommande(null, null, null, null, 0, 20);
    }
}
