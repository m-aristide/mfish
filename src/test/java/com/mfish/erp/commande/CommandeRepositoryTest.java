package com.mfish.erp.commande;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mfish.erp.TestcontainersConfiguration;
import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.config.ApplicationConfig;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import java.util.UUID;
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
class CommandeRepositoryTest {

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    private Client client() {
        return clientRepository.saveAndFlush(new Client("Awa", "+2250799999001"));
    }

    private Produit produit() {
        return produitRepository.saveAndFlush(new Produit("Attieke", 1000, "plat", true));
    }

    private Commande commandeAvecUneLigne() {
        final Commande commande = new Commande(client(), StatutCommande.A_PLANIFIER,
                ModePaiement.ESPECES, StatutPaiement.NON_PAYE, SourceCommande.MANUEL);
        commande.ajouterLigne(new LigneCommande(produit(), 2, 1000));
        commande.setMontantTotal(2000);
        return commande;
    }

    @Test
    @DisplayName("doit_persister_commande_avec_lignes_et_audit")
    void doit_persister_commande_avec_lignes_et_audit() {
        final Commande saved = commandeRepository.saveAndFlush(commandeAvecUneLigne());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getLignes()).hasSize(1);
    }

    @Test
    @DisplayName("doit_incrementer_version_sur_mise_a_jour")
    void doit_incrementer_version_sur_mise_a_jour() {
        final Commande saved = commandeRepository.saveAndFlush(commandeAvecUneLigne());

        saved.setStatut(StatutCommande.PLANIFIEE);
        final Commande updated = commandeRepository.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("doit_trouver_commandes_par_statut")
    void doit_trouver_commandes_par_statut() {
        commandeRepository.saveAndFlush(commandeAvecUneLigne());

        assertThat(commandeRepository.findByStatut(StatutCommande.A_PLANIFIER)).hasSize(1);
        assertThat(commandeRepository.findByStatut(StatutCommande.LIVREE)).isEmpty();
    }

    @Test
    @DisplayName("doit_supprimer_lignes_en_cascade_quand_commande_supprimee")
    void doit_supprimer_lignes_en_cascade_quand_commande_supprimee() {
        final Commande saved = commandeRepository.saveAndFlush(commandeAvecUneLigne());
        final UUID ligneId = saved.getLignes().get(0).getId();

        commandeRepository.delete(saved);
        commandeRepository.flush();

        assertThat(ligneCommandeRepository.findById(ligneId)).isEmpty();
    }

    @Test
    @DisplayName("doit_rejeter_montant_total_negatif")
    void doit_rejeter_montant_total_negatif() {
        final Commande commande = commandeAvecUneLigne();
        commande.setMontantTotal(-1);

        assertThatThrownBy(() -> commandeRepository.saveAndFlush(commande))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("doit_rejeter_quantite_nulle")
    void doit_rejeter_quantite_nulle() {
        final Commande commande = new Commande(client(), StatutCommande.A_PLANIFIER,
                ModePaiement.ESPECES, StatutPaiement.NON_PAYE, SourceCommande.MANUEL);
        commande.ajouterLigne(new LigneCommande(produit(), 0, 1000));
        commande.setMontantTotal(0);

        assertThatThrownBy(() -> commandeRepository.saveAndFlush(commande))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
