package com.mfish.erp.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.TestcontainersConfiguration;
import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.Commande;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.LigneCommande;
import com.mfish.erp.commande.LigneCommandeRepository;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commande.StatutPaiement;
import com.mfish.erp.config.ApplicationConfig;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ApplicationConfig.class})
class StatsServiceTest {

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private QuartierRepository quartierRepository;

    @Autowired
    private ProduitRepository produitRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(commandeRepository, ligneCommandeRepository);

        final Quartier cocody = quartierRepository.saveAndFlush(
                new Quartier("TestQuartier1", "Nord", 5.35, -3.97, 1, true));
        final Quartier plateau = quartierRepository.saveAndFlush(
                new Quartier("TestQuartier2", "Centre", 5.32, -4.01, 2, true));
        final Client client1 = client(1, cocody);
        final Client client2 = client(2, plateau);
        final Produit attieke = produitRepository.saveAndFlush(new Produit("Attieke", 1000, "plat", true));
        final Produit poulet = produitRepository.saveAndFlush(new Produit("Poulet", 5000, "piece", true));

        commande(client1, LocalDate.of(2026, 11, 1), StatutCommande.LIVREE, attieke, 2, 1000);
        commande(client2, LocalDate.of(2026, 11, 1), StatutCommande.PLANIFIEE, poulet, 1, 5000);
        commande(client1, LocalDate.of(2026, 11, 2), StatutCommande.LIVREE, attieke, 1, 1000);
        commande(client1, LocalDate.of(2026, 11, 2), StatutCommande.ANNULEE, attieke, 5, 1000);
    }

    private Client client(final int index, final Quartier quartier) {
        final Client client = new Client("Client" + index, "+225079999900" + index);
        client.setQuartier(quartier);
        return clientRepository.saveAndFlush(client);
    }

    private void commande(final Client client, final LocalDate date, final StatutCommande statut,
                          final Produit produit, final int quantite, final long prixUnitaire) {
        final Commande commande = new Commande(client, statut, ModePaiement.ESPECES,
                StatutPaiement.NON_PAYE, SourceCommande.MANUEL);
        commande.setDateLivraisonSouhaitee(date);
        commande.setMontantTotal((long) quantite * prixUnitaire);
        commande.ajouterLigne(new LigneCommande(produit, quantite, prixUnitaire));
        commandeRepository.saveAndFlush(commande);
    }

    @Test
    @DisplayName("doit_calculer_le_ca_sur_la_periode")
    void doit_calculer_le_ca_sur_la_periode() {
        assertThat(statsService.caParPeriode(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1)))
                .isEqualTo(7000);
    }

    @Test
    @DisplayName("doit_retourner_zero_pour_une_periode_vide")
    void doit_retourner_zero_pour_une_periode_vide() {
        assertThat(statsService.caParPeriode(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31)))
                .isZero();
    }

    @Test
    @DisplayName("doit_ignorer_les_commandes_annulees")
    void doit_ignorer_les_commandes_annulees() {
        assertThat(statsService.caParPeriode(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2)))
                .isEqualTo(1000);
    }

    @Test
    @DisplayName("doit_calculer_le_taux_de_livraison")
    void doit_calculer_le_taux_de_livraison() {
        assertThat(statsService.tauxLivraison(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1)))
                .isEqualTo(50.0);
    }

    @Test
    @DisplayName("doit_ordonner_les_top_produits")
    void doit_ordonner_les_top_produits() {
        final var tops = statsService.topProduits(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), 10);

        assertThat(tops).hasSize(2);
        assertThat(tops.get(0).produit()).isEqualTo("Attieke");
        assertThat(tops.get(0).quantiteTotale()).isEqualTo(3);
    }
}
