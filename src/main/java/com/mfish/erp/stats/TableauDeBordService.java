package com.mfish.erp.stats;

import com.mfish.erp.commande.CommandeDto;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.StatutCommande;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tableau de bord du commercant : livraisons du jour, encaissement, commandes non planifiees,
 * paiements en retard.
 */
@Service
@Transactional(readOnly = true)
public class TableauDeBordService {

    private final CommandeRepository commandeRepository;
    private final Clock clock;

    public TableauDeBordService(final CommandeRepository commandeRepository, final Clock clock) {
        this.commandeRepository = commandeRepository;
        this.clock = clock;
    }

    public List<CommandeDto> livraisonsDuJour() {
        final LocalDate aujourdhui = LocalDate.now(clock);
        return commandeRepository.findByDateLivraisonSouhaitee(aujourdhui).stream()
                .map(CommandeDto::from).toList();
    }

    public EncaissementDto encaisseRestant() {
        final long encaisse = commandeRepository.sommeMontantPaye();
        final long total = commandeRepository.sommeMontantTotalActif();
        return new EncaissementDto(encaisse, total - encaisse);
    }

    public List<CommandeDto> commandesNonPlanifiees() {
        return commandeRepository.findByStatut(StatutCommande.A_PLANIFIER).stream()
                .map(CommandeDto::from).toList();
    }

    public List<CommandeDto> paiementsEnRetard() {
        return commandeRepository.paiementsEnRetard().stream()
                .map(CommandeDto::from).toList();
    }
}
