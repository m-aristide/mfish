package com.mfish.erp.web.dto;

import jakarta.validation.constraints.Positive;

/**
 * Formulaire d'encaissement d'un montant sur une commande.
 */
public record PaiementFormulaire(@Positive(message = "validation.positif") long montant) {
}
