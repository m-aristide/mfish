package com.mfish.erp.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.client.ClientDto;
import com.mfish.erp.commande.CommandeDto;
import com.mfish.erp.commande.LigneCommandeDto;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commande.StatutPaiement;
import com.mfish.erp.produit.ProduitDto;
import com.mfish.erp.quartier.QuartierDto;
import com.mfish.erp.web.config.MonnaieProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VueMapperTest {

    private final VueMapper vueMapper = new VueMapper(new MonnaieMapper(new MonnaieProperties("FCFA")));

    @Test
    @DisplayName("assemble une vue de commande avec noms et montants formats")
    void assemble_une_vue_de_commande() {
        final UUID clientId = UUID.randomUUID();
        final UUID quartierId = UUID.randomUUID();
        final UUID produitId = UUID.randomUUID();
        final UUID commandeId = UUID.randomUUID();

        final CommandeDto commande = new CommandeDto(commandeId, clientId, StatutCommande.A_PLANIFIER,
                LocalDate.of(2026, 11, 5), 2_000, ModePaiement.ESPECES, StatutPaiement.NON_PAYE,
                0, null, SourceCommande.MANUEL,
                List.of(new LigneCommandeDto(UUID.randomUUID(), produitId, 2, 1_000)));

        final var vue = vueMapper.commandeVue(commande,
                Map.of(clientId, new ClientDto(clientId, "Awa", "+2250700000001", quartierId,
                        "Rue 1", null, null, null, null, false)),
                Map.of(quartierId, new QuartierDto(quartierId, "Cocody", "Nord", null, null, 1, true)),
                Map.of(produitId, new ProduitDto(produitId, "Attieke", 1_000, "plat", true, "/images/default-produit.svg")));

        assertThat(vue.clientNom()).isEqualTo("Awa");
        assertThat(vue.quartierNom()).isEqualTo("Cocody");
        assertThat(vue.montantTotalFormate()).isEqualTo("2 000 FCFA");
        assertThat(vue.restant()).isEqualTo(2_000);
        assertThat(vue.statut()).isEqualTo("statut.A_PLANIFIER");
        assertThat(vue.lignes()).hasSize(1);
        assertThat(vue.lignes().get(0).produitNom()).isEqualTo("Attieke");
        assertThat(vue.lignes().get(0).sousTotalFormate()).isEqualTo("2 000 FCFA");
    }
}
