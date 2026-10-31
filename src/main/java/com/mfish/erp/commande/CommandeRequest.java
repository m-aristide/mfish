package com.mfish.erp.commande;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Entree de creation d'une commande via l'API.
 */
public record CommandeRequest(
        @NotNull UUID clientId,
        @NotEmpty List<@Valid LigneCommandeRequest> lignes,
        @NotNull ModePaiement modePaiement,
        @NotNull SourceCommande source,
        LocalDate dateLivraisonSouhaitee,
        String notes) {
}
