package com.mfish.erp.web.dto;

import com.mfish.erp.commande.ModePaiement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Formulaire de creation d'une commande (le total est recalcule cote serveur).
 */
public record CommandeFormulaire(
        @NotNull(message = "validation.obligatoire") UUID clientId,
        @NotEmpty(message = "validation.auMoinsUneLigne") List<@Valid LigneCommandeFormulaire> lignes,
        @NotNull(message = "validation.obligatoire") ModePaiement modePaiement,
        LocalDate dateLivraisonSouhaitee,
        String notes) {
}
