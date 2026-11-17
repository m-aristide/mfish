package com.mfish.erp.stats;

import com.mfish.erp.commande.CommandeDto;
import java.util.List;

/**
 * Vue agregee du tableau de bord.
 */
public record TableauDeBordDto(
        List<CommandeDto> livraisonsDuJour,
        EncaissementDto encaissement,
        List<CommandeDto> commandesNonPlanifiees,
        List<CommandeDto> paiementsEnRetard) {
}
