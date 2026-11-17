package com.mfish.erp.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vue d'une tournee et de ses arrets ordonnes.
 */
public record TourneeVue(UUID id, LocalDate date, String statut, List<ArretVue> arrets) {
}
