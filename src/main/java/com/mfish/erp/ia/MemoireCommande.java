package com.mfish.erp.ia;

import com.mfish.erp.conversation.EtatCommande;
import com.mfish.erp.conversation.EtatCommandeJson;
import com.mfish.erp.conversation.LigneEtatCommande;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.springframework.stereotype.Component;

/**
 * Memoire de commande d'une conversation : fusionne ce que l'IA vient d'extraire avec l'etat
 * deja connu, sans jamais ecraser une information par du vide.
 *
 * <p>Regles de fusion :
 * <ul>
 *   <li>une valeur absente ou vide ne remplace jamais une valeur connue ;</li>
 *   <li>une valeur differente est une correction du client et remplace l'ancienne ;</li>
 *   <li>« ajoute », « en plus », « encore » ... additionnent la quantite au lieu de la remplacer ;</li>
 *   <li>une annulation remet la memoire a zero.</li>
 * </ul>
 */
@Component
public class MemoireCommande {

    /** Marqueurs d'ajout : la quantite s'additionne au lieu de remplacer la precedente. */
    private static final List<String> MARQUEURS_AJOUT = List.of(
            "ajoute", "ajouter", "rajoute", "rajouter", "en plus", "de plus",
            "encore", "aussi", "supplementaire");

    /** Reponses courtes valant confirmation d'un recapitulatif. */
    private static final List<String> MARQUEURS_CONFIRMATION = List.of(
            "oui", "ok", "d accord", "confirme", "c est bon", "valide", "exact", "parfait",
            "ca marche", "entendu");

    /** Au-dela de cette longueur, le message n'est plus une simple confirmation. */
    private static final int LONGUEUR_MAX_CONFIRMATION = 40;

    /** Longueur minimale pour tolerer une faute de frappe (distance de Levenshtein <= 1). */
    private static final int LONGUEUR_MIN_APPROCHANT = 4;

    /**
     * Fusionne la reponse de l'IA dans l'etat courant.
     */
    public EtatCommande fusionner(final EtatCommande courant, final ReponseIa reponse,
                                  final String message) {
        final EtatCommande etatCourant = courant == null ? EtatCommande.vide() : courant;
        if (reponse == null) {
            return etatCourant;
        }
        final Intention intention = reponse.intentionEnum();
        if (intention == Intention.ANNULATION) {
            return EtatCommande.vide();
        }
        final CommandeIa extrait = reponse.commande();
        if (extrait == null) {
            return etatCourant;
        }

        final LocalDate dateExtraite = EtatCommandeJson.analyserDate(extrait.dateSouhaitee());
        final EtatCommande depart = nouvelleCommande(etatCourant, intention, dateExtraite)
                ? EtatCommande.vide()
                : etatCourant;

        final boolean ajout = contientMarqueur(message, MARQUEURS_AJOUT);
        List<LigneEtatCommande> lignes = new ArrayList<>(depart.lignes());
        if (extrait.produits() != null) {
            for (final ProduitIa produit : extrait.produits()) {
                lignes = fusionnerLigne(lignes, produit, ajout);
            }
        }
        return new EtatCommande(lignes,
                valeurOuConservee(extrait.quartier(), depart.quartier()),
                valeurOuConservee(extrait.adresse(), depart.adresse()),
                dateExtraite == null ? depart.dateLivraison() : dateExtraite,
                valeurOuConservee(extrait.heureSouhaitee(), depart.heureLivraison()),
                depart.statut());
    }

    /** Recapitulatif d'une commande deja enregistree. */
    public String recapitulatif(final EtatCommande etat) {
        return "Votre commande est enregistree : " + etat.resume() + ".";
    }

    /** Recapitulatif d'une commande complete mais pas encore confirmee par le client. */
    public String recapitulatifAConfirmer(final EtatCommande etat) {
        return "Recapitulatif de votre commande : " + etat.resume()
                + ". Confirmez-vous cette commande ?";
    }

    /**
     * Question portant uniquement sur les informations encore manquantes, precedee d'un rappel
     * de ce qui a deja ete compris (le client ne doit jamais se repeter).
     */
    public String questionSuivante(final EtatCommande etat) {
        final String connu = etat.resume();
        final StringBuilder question = new StringBuilder();
        if (!connu.isBlank()) {
            question.append("J'ai bien note : ").append(connu).append(". ");
        }
        question.append("Il me manque ")
                .append(String.join(", ", etat.champsManquants()))
                .append(" pour enregistrer votre commande.");
        return question.toString();
    }

    /** Traduit l'etat memorise en commande exploitable par {@code OrchestrateurIa}. */
    public CommandeIa versCommandeIa(final EtatCommande etat) {
        final List<ProduitIa> produits = etat.lignes().stream()
                .map(ligne -> new ProduitIa(ligne.nom(), ligne.quantite()))
                .toList();
        return new CommandeIa(produits, etat.quartier(), etat.adresse(),
                etat.dateLivraison() == null ? null : etat.dateLivraison().toString(),
                etat.heureLivraison(), etat.complet());
    }

    /** Vrai si le message est une confirmation courte (« oui », « ok », « c'est bon »...). */
    public boolean estConfirmation(final String message) {
        final String normalise = normaliser(message);
        if (normalise.isBlank() || normalise.length() > LONGUEUR_MAX_CONFIRMATION) {
            return false;
        }
        return MARQUEURS_CONFIRMATION.stream().anyMatch(normalise::contains);
    }

    private static boolean nouvelleCommande(final EtatCommande etat, final Intention intention,
                                            final LocalDate dateExtraite) {
        return etat.estConfirmee()
                && intention == Intention.COMMANDE
                && dateExtraite != null
                && !dateExtraite.equals(etat.dateLivraison());
    }

    private static List<LigneEtatCommande> fusionnerLigne(final List<LigneEtatCommande> lignes,
                                                          final ProduitIa produit,
                                                          final boolean ajout) {
        if (produit == null) {
            return lignes;
        }
        final int quantite = Math.max(produit.quantite(), 0);
        final OptionalInt position = position(lignes, produit.nom());
        if (position.isEmpty()) {
            if (normaliser(produit.nom()).isBlank() || quantite <= 0) {
                return lignes;
            }
            final List<LigneEtatCommande> completees = new ArrayList<>(lignes);
            completees.add(new LigneEtatCommande(produit.nom(), quantite));
            return completees;
        }
        final int index = position.getAsInt();
        final LigneEtatCommande existante = lignes.get(index);
        final int nouvelleQuantite;
        if (quantite <= 0) {
            nouvelleQuantite = existante.quantite();
        } else if (ajout && existante.quantite() > 0) {
            nouvelleQuantite = existante.quantite() + quantite;
        } else {
            nouvelleQuantite = quantite;
        }
        final List<LigneEtatCommande> misesAJour = new ArrayList<>(lignes);
        misesAJour.set(index, new LigneEtatCommande(existante.nom(), nouvelleQuantite));
        return misesAJour;
    }

    /**
     * Position de la ligne correspondant au produit cite. Un nom vide designe l'unique ligne
     * existante (« ajoute 1 kilo » sans repriser le produit).
     */
    private static OptionalInt position(final List<LigneEtatCommande> lignes, final String nom) {
        final String recherche = normaliser(nom);
        if (recherche.isBlank()) {
            return lignes.size() == 1 ? OptionalInt.of(0) : OptionalInt.empty();
        }
        for (int i = 0; i < lignes.size(); i++) {
            final String candidat = normaliser(lignes.get(i).nom());
            if (candidat.equals(recherche)) {
                return OptionalInt.of(i);
            }
            final int longueurMin = Math.min(candidat.length(), recherche.length());
            if (longueurMin >= LONGUEUR_MIN_APPROCHANT
                    && SimilariteTexte.levenshtein(candidat, recherche) <= 1) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private static boolean contientMarqueur(final String message, final List<String> marqueurs) {
        final String normalise = normaliser(message);
        return marqueurs.stream().anyMatch(normalise::contains);
    }

    private static String valeurOuConservee(final String nouvelle, final String existante) {
        return nouvelle == null || nouvelle.isBlank() ? existante : nouvelle.trim();
    }

    /** Minuscules, sans accents, sans ponctuation et a espaces uniques. */
    private static String normaliser(final String valeur) {
        if (valeur == null) {
            return "";
        }
        return Normalizer.normalize(valeur.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
