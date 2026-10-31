package com.mfish.erp.stats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tableau-de-bord")
public class TableauDeBordController {

    private final TableauDeBordService tableauDeBordService;

    public TableauDeBordController(final TableauDeBordService tableauDeBordService) {
        this.tableauDeBordService = tableauDeBordService;
    }

    @GetMapping
    public TableauDeBordDto tableauDeBord() {
        return new TableauDeBordDto(
                tableauDeBordService.livraisonsDuJour(),
                tableauDeBordService.encaisseRestant(),
                tableauDeBordService.commandesNonPlanifiees(),
                tableauDeBordService.paiementsEnRetard());
    }
}
