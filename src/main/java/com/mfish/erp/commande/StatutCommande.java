package com.mfish.erp.commande;

/**
 * Cycle de vie d'une commande, avec les transitions autorisees entre etats.
 */
public enum StatutCommande {
    A_PLANIFIER,
    PLANIFIEE,
    EN_COURS,
    LIVREE,
    ANNULEE;

    /**
     * Indique si la transition vers {@code cible} est autorisee.
     * Les etats terminaux ({@link #LIVREE}, {@link #ANNULEE}) n'ont aucune sortie.
     */
    public boolean peutPasserA(StatutCommande cible) {
        return switch (this) {
            case A_PLANIFIER -> cible == PLANIFIEE || cible == ANNULEE;
            case PLANIFIEE -> cible == EN_COURS || cible == A_PLANIFIER || cible == ANNULEE;
            case EN_COURS -> cible == LIVREE || cible == PLANIFIEE;
            case LIVREE, ANNULEE -> false;
        };
    }
}
