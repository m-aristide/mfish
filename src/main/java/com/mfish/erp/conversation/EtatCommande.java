package com.mfish.erp.conversation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Etat structure d'une commande en cours de construction dans une conversation.
 *
 * <p>Cet etat est persiste en JSON (colonne {@code conversations.order_state}) afin de survivre
 * aux redemarrages : l'IA ne redemande jamais une information deja fournie.
 */
public record EtatCommande(
        List<LigneEtatCommande> lignes,
        String quartier,
        String adresse,
        LocalDate dateLivraison,
        String heureLivraison,
        StatutEtatCommande statut) {

    /** Libelles utilises dans les questions au client et dans le prompt. */
    public static final String CHAMP_QUANTITE = "la quantite";
    public static final String CHAMP_QUARTIER = "le quartier";
    public static final String CHAMP_DATE = "la date de livraison";

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public EtatCommande {
        lignes = lignes == null ? List.of() : List.copyOf(lignes);
        statut = statut == null ? StatutEtatCommande.EN_COURS : statut;
    }

    public static EtatCommande vide() {
        return new EtatCommande(List.of(), null, null, null, null, StatutEtatCommande.EN_COURS);
    }

    /**
     * Champs indispensables encore absents, dans l'ordre ou ils doivent etre demandes.
     */
    public List<String> champsManquants() {
        final List<String> manquants = new ArrayList<>();
        if (lignes.isEmpty() || lignes.stream().allMatch(ligne -> ligne.quantite() <= 0)) {
            manquants.add(CHAMP_QUANTITE);
        }
        if (estVide(quartier)) {
            manquants.add(CHAMP_QUARTIER);
        }
        if (dateLivraison == null) {
            manquants.add(CHAMP_DATE);
        }
        return List.copyOf(manquants);
    }

    public boolean complet() {
        return champsManquants().isEmpty();
    }

    public boolean estConfirmee() {
        return statut == StatutEtatCommande.CONFIRMEE;
    }

    public EtatCommande avecStatut(final StatutEtatCommande nouveauStatut) {
        return new EtatCommande(lignes, quartier, adresse, dateLivraison, heureLivraison,
                nouveauStatut);
    }

    /** Resume lisible des informations deja collectees (prompt et messages au client). */
    public String resume() {
        final List<String> parties = new ArrayList<>();
        lignes.stream()
                .filter(ligne -> ligne.quantite() > 0)
                .forEach(ligne -> parties.add(ligne.quantite() + " kg de " + ligne.nom()));
        if (!estVide(quartier)) {
            parties.add("quartier " + quartier);
        }
        if (!estVide(adresse)) {
            parties.add("adresse " + adresse);
        }
        if (dateLivraison != null) {
            parties.add("livraison le " + dateLivraison.format(FORMAT_DATE));
        }
        if (!estVide(heureLivraison)) {
            parties.add("vers " + heureLivraison);
        }
        return String.join(", ", parties);
    }

    private static boolean estVide(final String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
