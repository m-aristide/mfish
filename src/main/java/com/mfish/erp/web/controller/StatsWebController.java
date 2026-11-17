package com.mfish.erp.web.controller;

import com.mfish.erp.stats.StatsService;
import com.mfish.erp.web.dto.StatistiqueVue;
import com.mfish.erp.web.mapper.MonnaieMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Ecran « Statistiques » : CA, commandes, taux de livraison, top produits, quartiers, export CSV.
 */
@Controller
@RequestMapping("/statistiques")
public class StatsWebController {

    private final StatsService statsService;
    private final MonnaieMapper monnaieMapper;
    private final Clock clock;

    public StatsWebController(final StatsService statsService, final MonnaieMapper monnaieMapper,
                              final Clock clock) {
        this.statsService = statsService;
        this.monnaieMapper = monnaieMapper;
        this.clock = clock;
    }

    @GetMapping
    public String statistiques(@RequestParam(required = false) final LocalDate debut,
                               @RequestParam(required = false) final LocalDate fin,
                               final Model model) {
        final LocalDate finEffective = fin == null ? LocalDate.now(clock) : fin;
        final LocalDate debutEffective = debut == null ? finEffective.withDayOfMonth(1) : debut;
        final StatistiqueVue vue = construireVue(debutEffective, finEffective);
        model.addAttribute("vue", vue);
        model.addAttribute("debut", debutEffective);
        model.addAttribute("fin", finEffective);
        model.addAttribute("chartDataJson", chartDataJson(vue));
        model.addAttribute("pageActive", "statistiques");
        return "statistiques/index";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) final LocalDate debut,
                                         @RequestParam(required = false) final LocalDate fin) {
        final LocalDate finEffective = fin == null ? LocalDate.now(clock) : fin;
        final LocalDate debutEffective = debut == null ? finEffective.withDayOfMonth(1) : debut;
        final StatistiqueVue vue = construireVue(debutEffective, finEffective);
        final String csv = construireCsv(vue);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statistiques.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private StatistiqueVue construireVue(final LocalDate debut, final LocalDate fin) {
        final long ca = statsService.caParPeriode(debut, fin);
        final long nbCommandes = statsService.nbCommandes(debut, fin);
        final long taux = Math.round(statsService.tauxLivraison(debut, fin));
        final List<StatistiqueVue.ProduitStatistique> topProduits =
                statsService.topProduits(debut, fin, 10).stream()
                        .map(produit -> new StatistiqueVue.ProduitStatistique(produit.produit(),
                                produit.quantiteTotale()))
                        .toList();
        final List<StatistiqueVue.QuartierStatistique> quartiers =
                statsService.caParQuartier(debut, fin).stream()
                        .map(quartier -> new StatistiqueVue.QuartierStatistique(quartier.quartier(),
                                quartier.total(), monnaieMapper.formater(quartier.total())))
                        .toList();
        return new StatistiqueVue(debut + " → " + fin, ca, monnaieMapper.formater(ca),
                nbCommandes, taux, topProduits, quartiers);
    }

    private String construireCsv(final StatistiqueVue vue) {
        final StringBuilder csv = new StringBuilder();
        csv.append("chiffre_affaires;nombre_commandes;taux_livraison\n");
        csv.append(vue.ca()).append(';').append(vue.nbCommandes()).append(';')
                .append(vue.tauxLivraison()).append("\n\n");
        csv.append("produit;quantite\n");
        vue.topProduits().forEach(p -> csv.append(p.nom()).append(';').append(p.quantite()).append('\n'));
        csv.append("\nquartier;chiffre_affaires\n");
        vue.quartiers().forEach(q -> csv.append(q.nom()).append(';').append(q.ca()).append('\n'));
        return csv.toString();
    }

    private String chartDataJson(final StatistiqueVue vue) {
        final StringBuilder json = new StringBuilder("[");
        boolean premier = true;
        for (final StatistiqueVue.ProduitStatistique produit : vue.topProduits()) {
            if (!premier) {
                json.append(',');
            }
            json.append("{\"nom\":\"").append(echapperJson(produit.nom()))
                    .append("\",\"quantite\":").append(produit.quantite()).append('}');
            premier = false;
        }
        return json.append(']').toString();
    }

    private String echapperJson(final String valeur) {
        return valeur == null ? "" : valeur.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
