package com.mfish.erp.stats;

import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.LigneCommandeRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistiques de vente : chiffre d'affaires, top produits, taux de livraison.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    public StatsService(final CommandeRepository commandeRepository,
                        final LigneCommandeRepository ligneCommandeRepository) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
    }

    public long caParPeriode(final LocalDate debut, final LocalDate fin) {
        return commandeRepository.caParPeriode(debut, fin);
    }

    public List<CaParQuartier> caParQuartier(final LocalDate debut, final LocalDate fin) {
        return commandeRepository.caParQuartier(debut, fin);
    }

    public List<TopProduit> topProduits(final LocalDate debut, final LocalDate fin, final int limite) {
        return ligneCommandeRepository.topProduits(debut, fin, PageRequest.of(0, limite));
    }

    public double tauxLivraison(final LocalDate debut, final LocalDate fin) {
        final long total = commandeRepository.countCommandesPeriode(debut, fin);
        if (total == 0) {
            return 0.0;
        }
        final long livrees = commandeRepository.countCommandesLivrees(debut, fin);
        return (double) livrees / total * 100.0;
    }
}
