package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mfish.erp.client.Client;
import com.mfish.erp.commande.CommandeService;
import com.mfish.erp.commande.LigneCommande;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class OrchestrateurIaTest {

    @Mock
    private LlmPort llmPort;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private QuartierRepository quartierRepository;

    @Mock
    private CommandeService commandeService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneId.of("UTC"));

    private OrchestrateurIa orchestrateur() {
        return new OrchestrateurIa(llmPort, new PromptBuilder(), new ParseurReponseIa(),
                new RapprochementProduits(), produitRepository, quartierRepository, commandeService, clock);
    }

    private Client client() {
        return new Client("Awa", "+2250700000001");
    }

    @Test
    @DisplayName("doit_creer_une_commande_avec_le_prix_en_base_quand_complet")
    void doit_creer_une_commande_avec_le_prix_en_base_quand_complet() {
        when(produitRepository.findByActifTrueOrderByNomAsc())
                .thenReturn(List.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")));
        when(quartierRepository.findByActifTrueOrderByOrdreDefautAsc())
                .thenReturn(List.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)));
        final String json = "{\"reponse\":\"ok\",\"intention\":\"commande\","
                + "\"commande\":{\"produits\":[{\"nom\":\"Attieke\",\"quantite\":2}],"
                + "\"quartier\":\"Cocody\",\"adresse\":null,\"dateSouhaitee\":null,\"complet\":true},"
                + "\"escalade\":false}";
        when(llmPort.generer(anyString(), anyList())).thenReturn(json);

        final ResultatIa resultat = orchestrateur().traiter(client(), List.of(), "2 attieke");

        assertThat(resultat.commandeCreee()).isTrue();
        assertThat(resultat.escalade()).isFalse();

        final ArgumentCaptor<List<LigneCommande>> captor = ArgumentCaptor.forClass(List.class);
        verify(commandeService).creer(any(), captor.capture(), any(), any(), any(), any());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getPrixUnitaire()).isEqualTo(1000);
        assertThat(captor.getValue().get(0).getQuantite()).isEqualTo(2);
    }

    @Test
    @DisplayName("doit_ne_pas_creer_de_commande_quand_incomplet")
    void doit_ne_pas_creer_de_commande_quand_incomplet() {
        when(produitRepository.findByActifTrueOrderByNomAsc()).thenReturn(List.of());
        when(quartierRepository.findByActifTrueOrderByOrdreDefautAsc()).thenReturn(List.of());
        final String json = "{\"reponse\":\"il me faut plus d infos\",\"intention\":\"commande\","
                + "\"commande\":{\"produits\":[],\"quartier\":null,\"adresse\":null,"
                + "\"dateSouhaitee\":null,\"complet\":false},\"escalade\":false}";
        when(llmPort.generer(anyString(), anyList())).thenReturn(json);

        final ResultatIa resultat = orchestrateur().traiter(client(), List.of(), "je veux commander");

        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.escalade()).isFalse();
    }

    @Test
    @DisplayName("doit_escalader_si_l_ia_demande_escalade")
    void doit_escalader_si_l_ia_demande_escalade() {
        when(produitRepository.findByActifTrueOrderByNomAsc()).thenReturn(List.of());
        when(quartierRepository.findByActifTrueOrderByOrdreDefautAsc()).thenReturn(List.of());
        final String json = "{\"reponse\":\"reclamation\",\"intention\":\"autre\",\"commande\":null,\"escalade\":true}";
        when(llmPort.generer(anyString(), anyList())).thenReturn(json);

        final ResultatIa resultat = orchestrateur().traiter(client(), List.of(), "je me plains");

        assertThat(resultat.escalade()).isTrue();
    }

    @Test
    @DisplayName("doit_escalader_apres_deux_echecs_de_parsing")
    void doit_escalader_apres_deux_echecs_de_parsing() {
        when(produitRepository.findByActifTrueOrderByNomAsc()).thenReturn(List.of());
        when(quartierRepository.findByActifTrueOrderByOrdreDefautAsc()).thenReturn(List.of());
        when(llmPort.generer(anyString(), anyList())).thenReturn("pas-du-json");

        final ResultatIa resultat = orchestrateur().traiter(client(), List.of(), "bonjour");

        assertThat(resultat.escalade()).isTrue();
    }
}
