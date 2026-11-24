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
import com.mfish.erp.connaissance.ConnaissanceService;
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
    private ConnaissanceService connaissanceService;

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
        when(connaissanceService.contenuCourant()).thenReturn("BASE");
        return new OrchestrateurIa(llmPort, new PromptBuilder(connaissanceService),
                new ParseurReponseIa(), new MemoireCommande(clock), new RapprochementProduits(),
                new RapprochementQuartiers(), geocodingPort, produitRepository, quartierRepository,
                clientRepository, commandeService, clock);
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

    /** Reponse LLM avec un champ {@code reponse} vide : la reponse deterministe est utilisee. */
    private static String commandeJson(final String produit, final String quantite,
                                       final String quartier, final String date,
                                       final String intention) {
        return commandeJson("", produit, quantite, quartier, date, intention);
    }

    private static String commandeJson(final String reponse, final String produit,
                                       final String quantite, final String quartier,
                                       final String date, final String intention) {
        return "{\"reponse\":\"" + reponse + "\",\"intention\":\"" + intention + "\","
                + "\"commande\":{\"produits\":[{\"nom\":" + produit + ",\"quantite\":"
                + quantite + "}],\"quartier\":" + quartier + ",\"adresse\":null,"
                + "\"dateSouhaitee\":" + date + ",\"complet\":true},\"escalade\":false}";
    }

    private static String commandeNull() {
        return "{\"reponse\":\"\",\"intention\":\"autre\",\"commande\":null,\"escalade\":false}";
    }

    @Test
    @DisplayName("doit_sauvegarder_sur_confirmation_avec_le_prix_lu_en_base")
    void doit_sauvegarder_sur_confirmation_avec_le_prix_lu_en_base() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 2)), "Cocody", null, DATE_LIVRAISON,
                null, StatutEtatCommande.A_CONFIRMER);
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeNull());

        final ResultatIa resultat = orchestrateur().traiter(client(), memoire, List.of(), "oui");

        assertThat(resultat.commandeCreee()).isTrue();
        assertThat(resultat.escalade()).isFalse();
        assertThat(resultat.etat().estEnregistree()).isTrue();

        final ArgumentCaptor<List<LigneCommande>> captor = ArgumentCaptor.forClass(List.class);
        verify(commandeService).creer(any(), captor.capture(), any(), any(), any(), isNull());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getPrixUnitaire()).isEqualTo(1000);
        assertThat(captor.getValue().get(0).getQuantite()).isEqualTo(2);
    }

    @Test
    @DisplayName("doit_presenter_le_recapitulatif_quand_complet_sans_confirmation")
    void doit_presenter_le_recapitulatif_quand_complet_sans_confirmation() {
        catalogue();
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "2", "\"Cocody\"", "\"2026-11-19\"", "commande"));

        final ResultatIa resultat =
                orchestrateur().traiter(client(), List.of(), "2 attieke a Cocody");

        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.etat().estAConfirmer()).isTrue();
        assertThat(resultat.reponseAuClient()).contains("Recapitulatif");
        verify(commandeService, never()).creer(any(), any(), any(), any(), any(), any());
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

        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.etat().quartier()).isEqualTo("Cocody");
        assertThat(resultat.etat().dateLivraison()).isEqualTo(DATE_LIVRAISON);
        assertThat(resultat.etat().lignes().getFirst().quantite()).isEqualTo(3);
        assertThat(resultat.etat().estAConfirmer()).isTrue();
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
    @DisplayName("doit_surfaccer_la_reponse_naturelle_du_llm_a_une_question_de_prix")
    void doit_surfaccer_la_reponse_naturelle_du_llm_a_une_question_de_prix() {
        catalogue();
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "4 kg font 10 000 FCFA (2 500 FCFA/kg), plus 1 000 FCFA de livraison. Quel est votre quartier ?",
                "\"Attieke\"", "4", "null", "null", "commande"));

        final ResultatIa resultat =
                orchestrateur().traiter(client(), List.of(), "Je veux 4 kilo, ca fait combien ?");

        assertThat(resultat.reponseAuClient())
                .contains("10 000 FCFA")
                .doesNotContain("Il me manque");
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

        assertThat(resultat.etat().lignes().getFirst().quantite()).isEqualTo(5);
        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.etat().estAConfirmer()).isTrue();
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
    @DisplayName("doit_repondre_poliment_a_un_remerciement_apres_enregistrement")
    void doit_repondre_poliment_a_un_remerciement_apres_enregistrement() {
        catalogue();
        final EtatCommande enregistree = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, DATE_LIVRAISON,
                null, StatutEtatCommande.ENREGISTREE);
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeNull());

        final ResultatIa resultat =
                orchestrateur().traiter(client(), enregistree, List.of(), "Merci");

        assertThat(resultat.escalade()).isFalse();
        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.reponseAuClient()).contains("Merci").doesNotContain("enregistree");
    }

    @Test
    @DisplayName("doit_escalader_une_modification_d_une_commande_deja_enregistree")
    void doit_escalader_une_modification_d_une_commande_deja_enregistree() {
        catalogue();
        final EtatCommande enregistree = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, DATE_LIVRAISON,
                null, StatutEtatCommande.ENREGISTREE);
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeJson(
                "\"Attieke\"", "5", "\"Cocody\"", "\"2026-11-19\"", "modification"));

        final ResultatIa resultat =
                orchestrateur().traiter(client(), enregistree, List.of(), "mettez 5 kilos");

        assertThat(resultat.escalade()).isTrue();
        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.etat().lignes().getFirst().quantite()).isEqualTo(3);
    }

    @Test
    @DisplayName("doit_geocoder_et_enregistrer_un_quartier_inconnu")
    void doit_geocoder_et_enregistrer_un_quartier_inconnu() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 2)), "Bastos", null, DATE_LIVRAISON,
                null, StatutEtatCommande.A_CONFIRMER);
        when(geocodingPort.geocoder(anyString()))
                .thenReturn(Optional.of(new Coordonnees(3.8667, 11.5167)));
        when(quartierRepository.save(any(Quartier.class))).thenAnswer(inv -> inv.getArgument(0));
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeNull());

        final ResultatIa resultat = orchestrateur().traiter(client(), memoire, List.of(), "oui");

        assertThat(resultat.commandeCreee()).isTrue();
        assertThat(resultat.escalade()).isFalse();

        final ArgumentCaptor<Quartier> captor = ArgumentCaptor.forClass(Quartier.class);
        verify(quartierRepository).save(captor.capture());
        assertThat(captor.getValue().getNom()).isEqualTo("Bastos");
        assertThat(captor.getValue().getLatitude()).isEqualTo(3.8667);
        assertThat(captor.getValue().getLongitude()).isEqualTo(11.5167);
        verify(commandeService).creer(any(), any(), any(), any(), any(), isNull());
    }

    @Test
    @DisplayName("doit_enregistrer_la_commande_meme_sans_resultat_de_geocodage")
    void doit_enregistrer_la_commande_meme_sans_resultat_de_geocodage() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 2)), "QuartierFantome", null,
                DATE_LIVRAISON, null, StatutEtatCommande.A_CONFIRMER);
        when(geocodingPort.geocoder(anyString())).thenReturn(Optional.empty());
        when(quartierRepository.save(any(Quartier.class))).thenAnswer(inv -> inv.getArgument(0));
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeNull());

        final ResultatIa resultat =
                orchestrateur().traiter(client(), memoire, List.of(), "oui");

        assertThat(resultat.commandeCreee()).isTrue();
        assertThat(resultat.escalade()).isFalse();

        final ArgumentCaptor<Quartier> captor = ArgumentCaptor.forClass(Quartier.class);
        verify(quartierRepository).save(captor.capture());
        assertThat(captor.getValue().getNom()).isEqualTo("QuartierFantome");
        assertThat(captor.getValue().getLatitude()).isNull();
        verify(commandeService).creer(any(), any(), any(), any(), any(), isNull());
    }

    @Test
    @DisplayName("doit_enregistrer_la_commande_meme_si_le_geocodage_est_indisponible")
    void doit_enregistrer_la_commande_meme_si_le_geocodage_est_indisponible() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 2)), "Bastos", null, DATE_LIVRAISON,
                null, StatutEtatCommande.A_CONFIRMER);
        when(geocodingPort.geocoder(anyString()))
                .thenThrow(new AppelExterneException("Google Geocoding a repondu 403 FORBIDDEN"));
        when(quartierRepository.save(any(Quartier.class))).thenAnswer(inv -> inv.getArgument(0));
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeNull());

        final ResultatIa resultat = orchestrateur().traiter(client(), memoire, List.of(), "oui");

        assertThat(resultat.commandeCreee()).isTrue();
        assertThat(resultat.escalade()).isFalse();
        verify(commandeService).creer(any(), any(), any(), any(), any(), isNull());
    }

    @Test
    @DisplayName("doit_repondre_honnetement_sans_pretendre_reussir_sur_produit_inconnu")
    void doit_repondre_honnetement_sans_pretendre_reussir_sur_produit_inconnu() {
        catalogue();
        final EtatCommande memoire = new EtatCommande(
                List.of(new LigneEtatCommande("ProduitX", 2)), "Cocody", null, DATE_LIVRAISON,
                null, StatutEtatCommande.A_CONFIRMER);
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeNull());

        final ResultatIa resultat = orchestrateur().traiter(client(), memoire, List.of(), "oui");

        assertThat(resultat.commandeCreee()).isFalse();
        assertThat(resultat.escalade()).isTrue();
        assertThat(resultat.etat().statut()).isEqualTo(StatutEtatCommande.ECHEC_ENREGISTREMENT);
        assertThat(resultat.reponseAuClient())
                .contains("probleme technique")
                .contains("2 kg de ProduitX")
                .doesNotContain("enregistree");
    }
}
