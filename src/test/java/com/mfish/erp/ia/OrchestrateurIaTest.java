package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.CommandeService;
import com.mfish.erp.commande.LigneCommande;
import com.mfish.erp.commun.AppelExterneException;
import com.mfish.erp.conversation.EtatCommande;
import com.mfish.erp.conversation.LigneEtatCommande;
import com.mfish.erp.conversation.StatutEtatCommande;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import com.mfish.erp.tournee.Coordonnees;
import com.mfish.erp.tournee.GeocodingPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class OrchestrateurIaTest {

    private static final LocalDate DATE_LIVRAISON = LocalDate.of(2026, 11, 19);

    @Mock
    private LlmPort llmPort;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private QuartierRepository quartierRepository;

    @Mock
    private GeocodingPort geocodingPort;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CommandeService commandeService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"),
            ZoneId.of("UTC"));

    private OrchestrateurIa orchestrateur() {
        return new OrchestrateurIa(llmPort, new PromptBuilder(), new ParseurReponseIa(),
                new MemoireCommande(), new RapprochementProduits(), new RapprochementQuartiers(),
                geocodingPort, produitRepository, quartierRepository, clientRepository,
                commandeService, clock);
    }

    private Client client() {
        return new Client("Awa", "+2250700000001");
    }

    private void catalogue() {
        when(produitRepository.findByActifTrueOrderByNomAsc())
                .thenReturn(List.of(new Produit("Attieke", 1000, "plat", true, "")));
        when(quartierRepository.findByActifTrueOrderByOrdreDefautAsc())
                .thenReturn(List.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)));
    }

    private static String commandeJson(final String produit, final String quantite,
                                       final String quartier, final String date,
                                       final String intention) {
        return "{\"reponse\":\"ok\",\"intention\":\"" + intention + "\","
                + "\"commande\":{\"produits\":[{\"nom\":" + produit + ",\"quantite\":"
                + quantite + "}],\"quartier\":" + quartier + ",\"adresse\":null,"
                + "\"dateSouhaitee\":" + date + ",\"complet\":true},\"escalade\":false}";
    }

    @Test
    @DisplayName("doit_creer_une_commande_avec_le_prix_en_base_quand_complet")
    void doit_creer_une_commande_avec_le_prix_en_base_quand_complet() {
        catalogue();
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "2", "\"Cocody\"", "\"2026-11-19\"", "commande"));

        final ResultatIa resultat = orchestrateur().traiter(client(), List.of(), "2 attieke");

        assertThat(resultat.commandeCreee()).isTrue();
        assertThat(resultat.escalade()).isFalse();
        assertThat(resultat.etat().estConfirmee()).isTrue();

        final ArgumentCaptor<List<LigneCommande>> captor = ArgumentCaptor.forClass(List.class);
        verify(commandeService).creer(any(), captor.capture(), any(), any(), any(), isNull());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getPrixUnitaire()).isEqualTo(1000);
        assertThat(captor.getValue().get(0).getQuantite()).isEqualTo(2);
    }

    @Test
    @DisplayName("doit_reutiliser_les_informations_collectees_dans_les_messages_precedents")
    void doit_reutiliser_les_informations_collectees_dans_les_messages_precedents() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, null, null,
                StatutEtatCommande.EN_COURS);
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "null", "0", "null", "\"2026-11-19\"", "commande"));

        final ResultatIa resultat =
                orchestrateur().traiter(client(), memoire, List.of(), "le 19/11/2026");

        assertThat(resultat.commandeCreee()).isTrue();
        final ArgumentCaptor<List<LigneCommande>> captor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<LocalDate> date = ArgumentCaptor.forClass(LocalDate.class);
        verify(commandeService).creer(any(), captor.capture(), any(), any(), date.capture(),
                isNull());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getQuantite()).isEqualTo(3);
        assertThat(date.getValue()).isEqualTo(DATE_LIVRAISON);
        assertThat(resultat.etat().quartier()).isEqualTo("Cocody");
    }

    @Test
    @DisplayName("doit_ne_demander_que_les_informations_encore_manquantes")
    void doit_ne_demander_que_les_informations_encore_manquantes() {
        catalogue();
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "3", "null", "null", "commande"));

        final ResultatIa resultat = orchestrateur().traiter(client(), List.of(), "3 attieke");

        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.reponseAuClient())
                .contains("J'ai bien note")
                .contains("3 kg de Attieke")
                .contains("le quartier")
                .contains("la date de livraison")
                .doesNotContain("le produit");
        assertThat(resultat.etat().lignes()).hasSize(1);
        verify(commandeService, never()).creer(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("doit_remplacer_la_quantite_quand_le_client_corrige")
    void doit_remplacer_la_quantite_quand_le_client_corrige() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, null, null,
                StatutEtatCommande.EN_COURS);
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "5", "null", "\"2026-11-19\"", "modification"));

        final ResultatIa resultat =
                orchestrateur().traiter(client(), memoire, List.of(), "non, mettez 5 kilos");

        assertThat(resultat.commandeCreee()).isTrue();
        final ArgumentCaptor<List<LigneCommande>> captor = ArgumentCaptor.forClass(List.class);
        verify(commandeService).creer(any(), captor.capture(), any(), any(), any(), isNull());
        assertThat(captor.getValue().get(0).getQuantite()).isEqualTo(5);
    }

    @Test
    @DisplayName("doit_additionner_la_quantite_quand_le_client_ajoute")
    void doit_additionner_la_quantite_quand_le_client_ajoute() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), null, null, null, null,
                StatutEtatCommande.EN_COURS);
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "1", "null", "null", "modification"));

        final ResultatIa resultat =
                orchestrateur().traiter(client(), memoire, List.of(), "ajoute 1 kilo");

        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.etat().lignes().getFirst().quantite()).isEqualTo(4);
    }

    @Test
    @DisplayName("doit_creer_la_commande_quand_le_client_confirme_le_recapitulatif")
    void doit_creer_la_commande_quand_le_client_confirme_le_recapitulatif() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, DATE_LIVRAISON,
                null, StatutEtatCommande.EN_COURS);
        when(llmPort.generer(anyString(), anyList()))
                .thenReturn("{\"reponse\":\"Tres bien.\",\"intention\":\"autre\","
                        + "\"commande\":null,\"escalade\":false}");

        final ResultatIa resultat = orchestrateur().traiter(client(), memoire, List.of(), "oui");

        assertThat(resultat.commandeCreee()).isTrue();
        assertThat(resultat.etat().estConfirmee()).isTrue();
    }

    @Test
    @DisplayName("doit_repondre_poliment_a_un_remerciement_apres_confirmation")
    void doit_repondre_poliment_a_un_remerciement_apres_confirmation() {
        catalogue();
        final EtatCommande confirmee = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, DATE_LIVRAISON,
                null, StatutEtatCommande.CONFIRMEE);
        when(llmPort.generer(anyString(), anyList())).thenReturn(
                "{\"reponse\":\"ok\",\"intention\":\"autre\",\"commande\":null,\"escalade\":false}");

        final ResultatIa resultat =
                orchestrateur().traiter(client(), confirmee, List.of(), "Merci");

        assertThat(resultat.escalade()).isFalse();
        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.reponseAuClient()).contains("Merci").doesNotContain("enregistree");
    }

    @Test
    @DisplayName("doit_escalader_une_modification_d_une_commande_deja_enregistree")
    void doit_escalader_une_modification_d_une_commande_deja_enregistree() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, DATE_LIVRAISON,
                null, StatutEtatCommande.CONFIRMEE);
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "5", "\"Cocody\"", "\"2026-11-19\"", "modification"));

        final ResultatIa resultat =
                orchestrateur().traiter(client(), memoire, List.of(), "mettez 5 kilos");

        assertThat(resultat.escalade()).isTrue();
        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.etat().lignes().getFirst().quantite()).isEqualTo(3);
    }

    @Test
    @DisplayName("doit_geocoder_et_enregistrer_un_quartier_inconnu")
    void doit_geocoder_et_enregistrer_un_quartier_inconnu() {
        catalogue();
        when(geocodingPort.geocoder(anyString()))
                .thenReturn(Optional.of(new Coordonnees(3.8667, 11.5167)));
        when(quartierRepository.save(any(Quartier.class))).thenAnswer(inv -> inv.getArgument(0));
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "2", "\"Bastos\"", "\"2026-11-19\"", "commande"));

        final ResultatIa resultat =
                orchestrateur().traiter(client(), List.of(), "2 attieke a Bastos");

        assertThat(resultat.commandeCreee()).isTrue();

        final ArgumentCaptor<Quartier> captor = ArgumentCaptor.forClass(Quartier.class);
        verify(quartierRepository).save(captor.capture());
        assertThat(captor.getValue().getNom()).isEqualTo("Bastos");
        assertThat(captor.getValue().getLatitude()).isEqualTo(3.8667);
        assertThat(captor.getValue().getLongitude()).isEqualTo(11.5167);
        verify(commandeService).creer(any(), any(), any(), any(), any(), isNull());
    }

    @Test
    @DisplayName("doit_escalader_quand_le_geocodage_ne_trouve_rien")
    void doit_escalader_quand_le_geocodage_ne_trouve_rien() {
        catalogue();
        when(geocodingPort.geocoder(anyString())).thenReturn(Optional.empty());
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "2", "\"QuartierFantome\"", "\"2026-11-19\"", "commande"));

        final ResultatIa resultat = orchestrateur().traiter(client(), List.of(), "2 attieke");

        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.escalade()).isTrue();
        verify(quartierRepository, never()).save(any(Quartier.class));
        verify(commandeService, never()).creer(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("doit_escalader_quand_le_geocodage_est_indisponible")
    void doit_escalader_quand_le_geocodage_est_indisponible() {
        catalogue();
        when(geocodingPort.geocoder(anyString()))
                .thenThrow(new AppelExterneException("Google Geocoding a repondu 403 FORBIDDEN"));
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "2", "\"Bastos\"", "\"2026-11-19\"", "commande"));

        final ResultatIa resultat = orchestrateur().traiter(client(), List.of(), "2 attieke a Bastos");

        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.escalade()).isTrue();
        verify(quartierRepository, never()).save(any(Quartier.class));
        verify(commandeService, never()).creer(any(), any(), any(), any(), any(), any());
    }
}
