package com.mfish.erp.commande;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mfish.erp.client.Client;
import com.mfish.erp.produit.Produit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommandeServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @InjectMocks
    private CommandeService service;

    private Client client() {
        return new Client("Awa", "+2250700000001");
    }

    private LigneCommande ligne(long prixUnitaire, int quantite) {
        return new LigneCommande(new Produit("Produit", prixUnitaire, "piece", true, "/images/default-produit.svg"), quantite, prixUnitaire);
    }

    private Commande commandeDeTotal(long total) {
        final Commande commande = new Commande(client(), StatutCommande.A_PLANIFIER,
                ModePaiement.ESPECES, StatutPaiement.NON_PAYE, SourceCommande.MANUEL);
        commande.setMontantTotal(total);
        return commande;
    }

    @Test
    @DisplayName("doit_calculer_le_total_a_partir_des_lignes")
    void doit_calculer_le_total_a_partir_des_lignes() {
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));

        final Commande commande = service.creer(client(),
                List.of(ligne(1000, 2), ligne(500, 3)),
                ModePaiement.ESPECES, SourceCommande.MANUEL);

        assertThat(commande.getMontantTotal()).isEqualTo(2L * 1000 + 3L * 500);
        assertThat(commande.getStatut()).isEqualTo(StatutCommande.A_PLANIFIER);
        assertThat(commande.getLignes()).hasSize(2);
    }

    @Test
    @DisplayName("doit_marquer_comme_deja_payee_si_mode_deja_paye")
    void doit_marquer_comme_deja_payee_si_mode_deja_paye() {
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));

        final Commande commande = service.creer(client(),
                List.of(ligne(1000, 1)),
                ModePaiement.DEJA_PAYE, SourceCommande.MANUEL);

        assertThat(commande.getStatutPaiement()).isEqualTo(StatutPaiement.PAYE);
        assertThat(commande.getMontantPaye()).isEqualTo(1000);
    }

    @Test
    @DisplayName("doit_refuser_commande_sans_ligne")
    void doit_refuser_commande_sans_ligne() {
        assertThatThrownBy(() -> service.creer(client(), List.of(),
                ModePaiement.ESPECES, SourceCommande.MANUEL))
                .isInstanceOf(CommandeInvalideException.class);
    }

    @Test
    @DisplayName("doit_refuser_quantite_nulle")
    void doit_refuser_quantite_nulle() {
        assertThatThrownBy(() -> service.creer(client(), List.of(ligne(1000, 0)),
                ModePaiement.ESPECES, SourceCommande.MANUEL))
                .isInstanceOf(CommandeInvalideException.class);
    }

    @Test
    @DisplayName("doit_enregistrer_paiement_partiel_puis_complet")
    void doit_enregistrer_paiement_partiel_puis_complet() {
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        final Commande commande = commandeDeTotal(2000);

        service.enregistrerPaiement(commande, 800);
        assertThat(commande.getMontantPaye()).isEqualTo(800);
        assertThat(commande.getStatutPaiement()).isEqualTo(StatutPaiement.PARTIEL);

        service.enregistrerPaiement(commande, 1200);
        assertThat(commande.getMontantPaye()).isEqualTo(2000);
        assertThat(commande.getStatutPaiement()).isEqualTo(StatutPaiement.PAYE);
    }

    @Test
    @DisplayName("doit_refuser_surpaiement")
    void doit_refuser_surpaiement() {
        final Commande commande = commandeDeTotal(2000);

        assertThatThrownBy(() -> service.enregistrerPaiement(commande, 2001))
                .isInstanceOf(SurpaiementException.class);
    }

    @Test
    @DisplayName("doit_refuser_transition_invalide")
    void doit_refuser_transition_invalide() {
        final Commande commande = commandeDeTotal(2000);

        assertThatThrownBy(() -> service.changerStatut(commande, StatutCommande.LIVREE))
                .isInstanceOf(TransitionStatutInvalideException.class);
    }

    @Test
    @DisplayName("doit_refuser_annulation_d_une_commande_livree")
    void doit_refuser_annulation_d_une_commande_livree() {
        final Commande commande = commandeDeTotal(2000);
        commande.setStatut(StatutCommande.LIVREE);

        assertThatThrownBy(() -> service.annuler(commande))
                .isInstanceOf(TransitionStatutInvalideException.class);
    }
}
