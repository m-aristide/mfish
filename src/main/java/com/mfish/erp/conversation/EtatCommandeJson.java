package com.mfish.erp.conversation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serialisation de {@link EtatCommande} vers la colonne {@code conversations.order_state}.
 *
 * <p>Format tolerant : un JSON absent, illisible ou partiel degrade vers un etat en cours plutot
 * que de faire echouer le traitement du message. Le champ derive {@code champsManquants} est ecrit
 * pour le diagnostic mais ignore a la lecture.
 */
public final class EtatCommandeJson {

    private static final DateTimeFormatter FORMAT_DATE_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private EtatCommandeJson() {
    }

    public static String versJson(final EtatCommande etat) {
        final EtatCommande valeur = etat == null ? EtatCommande.vide() : etat;
        return MAPPER.writeValueAsString(EtatCommandeDto.depuis(valeur));
    }

    public static EtatCommande depuisJson(final String json) {
        if (json == null || json.isBlank()) {
            return EtatCommande.vide();
        }
        try {
            final EtatCommandeDto dto = MAPPER.readValue(json, EtatCommandeDto.class);
            return dto == null ? EtatCommande.vide() : dto.versEtat();
        } catch (JacksonException e) {
            // Etat illisible : on repart d'une commande vide, la conversation continue normalement.
            return EtatCommande.vide();
        }
    }

    /**
     * Analyse une date au format ISO ({@code 2026-11-19}) ou francais ({@code 19/11/2026}).
     * Retourne {@code null} si la valeur est absente ou non reconnue.
     */
    public static LocalDate analyserDate(final String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        final String texte = valeur.trim();
        try {
            return LocalDate.parse(texte);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(texte, FORMAT_DATE_FR);
            } catch (DateTimeParseException e2) {
                // Format inconnu : la date reste a collecter, elle sera redemandee au client.
                return null;
            }
        }
    }

    static StatutEtatCommande analyserStatut(final String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return StatutEtatCommande.EN_COURS;
        }
        try {
            return StatutEtatCommande.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return StatutEtatCommande.EN_COURS;
        }
    }

    /** Representation persistee : listes de chaines, aucune dependance a un dialecte SQL. */
    public record EtatCommandeDto(
            List<LigneDto> lignes,
            String quartier,
            String adresse,
            String dateLivraison,
            String heureLivraison,
            String statut,
            List<String> champsManquants) {

        static EtatCommandeDto depuis(final EtatCommande etat) {
            final List<LigneDto> lignes = etat.lignes().stream()
                    .map(ligne -> new LigneDto(ligne.nom(), ligne.quantite()))
                    .toList();
            return new EtatCommandeDto(lignes, etat.quartier(), etat.adresse(),
                    etat.dateLivraison() == null ? null : etat.dateLivraison().toString(),
                    etat.heureLivraison(), etat.statut().name(), etat.champsManquants());
        }

        EtatCommande versEtat() {
            final List<LigneEtatCommande> lignesEtat = lignes == null
                    ? List.of()
                    : lignes.stream()
                            .map(ligne -> new LigneEtatCommande(ligne.nom(), ligne.quantite()))
                            .toList();
            return new EtatCommande(lignesEtat, quartier, adresse, analyserDate(dateLivraison),
                    heureLivraison, analyserStatut(statut));
        }
    }

    public record LigneDto(String nom, int quantite) {
    }
}
