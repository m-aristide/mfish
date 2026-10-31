package com.mfish.erp.tournee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mfish.erp.TestcontainersConfiguration;
import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.Commande;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commande.StatutPaiement;
import com.mfish.erp.config.ApplicationConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ApplicationConfig.class})
class TourneeRepositoryTest {

    @Autowired
    private TourneeRepository tourneeRepository;

    @Autowired
    private ArretTourneeRepository arretTourneeRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Commande commande(String telephone) {
        final Client client = clientRepository.saveAndFlush(new Client("Awa", telephone));
        return commandeRepository.saveAndFlush(new Commande(client, StatutCommande.A_PLANIFIER,
                ModePaiement.ESPECES, StatutPaiement.NON_PAYE, SourceCommande.MANUEL));
    }

    @Test
    @DisplayName("doit_rejeter_date_dupliquee")
    void doit_rejeter_date_dupliquee() {
        final LocalDate date = LocalDate.of(2026, 11, 1);
        tourneeRepository.saveAndFlush(new Tournee(date, StatutTournee.BROUILLON));

        assertThatThrownBy(() -> tourneeRepository.saveAndFlush(new Tournee(date, StatutTournee.BROUILLON)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("doit_rejeter_commande_deux_fois_dans_la_meme_tournee")
    void doit_rejeter_commande_deux_fois_dans_la_meme_tournee() {
        final Tournee tournee = tourneeRepository.saveAndFlush(
                new Tournee(LocalDate.of(2026, 11, 1), StatutTournee.BROUILLON));
        final Commande commande = commande("+2250799999001");

        arretTourneeRepository.saveAndFlush(new ArretTournee(tournee, commande, 0, StatutArret.A_LIVRER));

        assertThatThrownBy(() -> arretTourneeRepository
                .saveAndFlush(new ArretTournee(tournee, commande, 1, StatutArret.A_LIVRER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("doit_trouver_arrets_ordonnes_par_position")
    void doit_trouver_arrets_ordonnes_par_position() {
        final Tournee tournee = tourneeRepository.saveAndFlush(
                new Tournee(LocalDate.of(2026, 11, 1), StatutTournee.BROUILLON));
        arretTourneeRepository.saveAndFlush(
                new ArretTournee(tournee, commande("+2250799999001"), 1, StatutArret.A_LIVRER));
        arretTourneeRepository.saveAndFlush(
                new ArretTournee(tournee, commande("+2250799999002"), 0, StatutArret.A_LIVRER));

        final List<ArretTournee> arrets =
                arretTourneeRepository.findByTourneeIdOrderByPositionAsc(tournee.getId());

        assertThat(arrets).extracting(ArretTournee::getPosition).containsExactly(0, 1);
    }
}
