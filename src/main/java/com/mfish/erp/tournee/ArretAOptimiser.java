package com.mfish.erp.tournee;

import java.util.UUID;

/**
 * Arret a ordonnancer (commande, coordonnees et ordre par defaut du quartier).
 */
public record ArretAOptimiser(UUID commandeId, Coordonnees coordonnees, int ordreQuartier) {
}
