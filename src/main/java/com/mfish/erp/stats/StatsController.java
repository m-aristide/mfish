package com.mfish.erp.stats;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(final StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/ca")
    public long caParPeriode(@RequestParam final LocalDate debut, @RequestParam final LocalDate fin) {
        return statsService.caParPeriode(debut, fin);
    }

    @GetMapping("/ca-par-quartier")
    public List<CaParQuartier> caParQuartier(@RequestParam final LocalDate debut,
                                             @RequestParam final LocalDate fin) {
        return statsService.caParQuartier(debut, fin);
    }

    @GetMapping("/top-produits")
    public List<TopProduit> topProduits(@RequestParam final LocalDate debut,
                                        @RequestParam final LocalDate fin,
                                        @RequestParam(defaultValue = "10") final int limite) {
        return statsService.topProduits(debut, fin, limite);
    }

    @GetMapping("/taux-livraison")
    public double tauxLivraison(@RequestParam final LocalDate debut, @RequestParam final LocalDate fin) {
        return statsService.tauxLivraison(debut, fin);
    }
}
