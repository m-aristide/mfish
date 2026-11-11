package com.mfish.erp.tournee;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.Commande;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commande.StatutPaiement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifie qu'un arret renvoye par {@link TourneeService#arrets} expose sa commande et son
 * client hors transaction (l'interface web est rendue avec {@code open-in-view=false}).
 */
@SpringBootTest
@ActiveProfiles("test")
class TourneeServiceIntegrationTest {

    @Autowired
    private TourneeService tourneeService;

    @Autowired
    private TourneeRepository tourneeRepository;

    @Autowired
    private ArretTourneeRepository arretTourneeRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("les arrets chargent la commande et le client hors transaction")
    void les_arrets_chargent_la_commande_et_le_client_hors_transaction() {
        final Client client = clientRepository.saveAndFlush(new Client("Awa Test", "+2250700000099"));
        final Commande commande = commandeRepository.saveAndFlush(new Commande(client,
                StatutCommande.A_PLANIFIER, ModePaiement.ESPECES, StatutPaiement.NON_PAYE,
                SourceCommande.MANUEL));
        final Tournee tournee = tourneeRepository.saveAndFlush(
                new Tournee(LocalDate.of(2026, 11, 1), StatutTournee.BROUILLON));
        arretTourneeRepository.saveAndFlush(
                new ArretTournee(tournee, commande, 0, StatutArret.A_LIVRER));

        final List<ArretTournee> arrets = tourneeService.arrets(tournee.getId());

        assertThat(arrets).hasSize(1);
        assertThat(arrets.get(0).getCommande().getClient().getId()).isEqualTo(client.getId());
        assertThat(arrets.get(0).getCommande().getClient().getNom()).isEqualTo("Awa Test");
    }
}