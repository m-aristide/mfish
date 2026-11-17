package com.mfish.erp.commande;

import jakarta.validation.constraints.Positive;

/**
 * Encaissement d'un montant sur une commande.
 */
public record PaiementRequest(@Positive long montant) {
}
