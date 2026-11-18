package com.mfish.erp.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.Commande;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commande.StatutPaiement;
import com.mfish.erp.conversation.Conversation;
import com.mfish.erp.conversation.ConversationRepository;
import com.mfish.erp.conversation.ModeConversation;
import com.mfish.erp.ia.LlmPort;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import com.mfish.erp.simulateur.SimulateurMessageStore;
import com.mfish.erp.tournee.ArretTournee;
import com.mfish.erp.tournee.ArretTourneeRepository;
import com.mfish.erp.tournee.RoutingPort;
import com.mfish.erp.tournee.StatutArret;
import com.mfish.erp.tournee.StatutTournee;
import com.mfish.erp.tournee.Tournee;
import com.mfish.erp.tournee.TourneeRepository;
import com.mfish.erp.webhook.EvenementTraiteRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Scenario de bout en bout : conversation WhatsApp d'un client (webhook Chatwoot signe),
 * reponse de l'IA, creation du client et de la commande, puis calcul de la tournee du jour
 * de livraison.
 *
 * <p>Determinisme : le LLM est remplace par un stub ({@link LlmPort}) qui rejoue deux reponses
 * JSON figees, conformes au contrat de {@code ParseurReponseIa}. RouteXL est simule indisponible
 * afin d'exercer le plan B local ({@code OptimisationLocale}), ce qui fixe l'heure d'arrivee.
 *
 * <p>Scenario rejoue :
 * <ol>
 *   <li>« Vous avez du poisson silure frais ? » : reponse de l'IA, aucune commande ;</li>
 *   <li>« Je prends 3 kg ... le 19/11/2026 a 12h a Obobogo » : client + commande ;</li>
 *   <li>calcul de la tournee du 19/11/2026 : un arret a 08:02.</li>
 * </ol>
 */
@SpringBootTest(properties = {
        "app.simulateur.active=true",
        "app.simulateur.mode=LOCAL"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScenarioCommandeTourneeE2ETest {

    private static final String SECRET = "test-webhook-secret";
    private static final String CONVERSATION_ID = "conversation-scenario-e2e";
    private static final String EVENEMENT_1 = "evt-scenario-e2e-1";
    private static final String EVENEMENT_2 = "evt-scenario-e2e-2";
    private static final String TELEPHONE = "+237690112233";
    private static final String NOM_CLIENT = "Awa Ndongo";
    private static final String NOM_QUARTIER = "Obobogo";
    private static final String NOM_PRODUIT = "Poisson silure frais";
    private static final long PRIX_KG = 2500L;
    private static final double LATITUDE_QUARTIER = 3.8179466;
    private static final double LONGITUDE_QUARTIER = 11.4946833;
    private static final LocalDate DATE_LIVRAISON = LocalDate.of(2026, 11, 19);
    private static final LocalTime HEURE_ARRIVEE_ATTENDUE = LocalTime.of(8, 2);

    private static final String QUESTION =
            "Bonjour, est-ce que vous avez du poisson silure frais ?";
    private static final String MESSAGE_COMMANDE = "Je prends 3 kilos de poisson silure frais, "
            + "livraison le 19/11/2026 a 12h a Obobogo, rue 1.234 maison 12.";

    private static final String REPONSE_QUESTION = "{\"reponse\":\"Oui, nous avons du poisson "
            + "silure frais a 2500 FCFA le kilo.\",\"intention\":\"question\","
            + "\"commande\":null,\"escalade\":false}";

    private static final String REPONSE_COMMANDE = "{\"reponse\":\"C'est note : 3 kg de poisson "
            + "silure frais, livraison le 19/11/2026 a Obobogo.\",\"intention\":\"commande\","
            + "\"commande\":{\"produits\":[{\"nom\":\"poisson silure frais\",\"quantite\":3}],"
            + "\"quartier\":\"Obobogo\",\"adresse\":\"Rue 1.234, maison 12\","
            + "\"dateSouhaitee\":\"2026-11-19\",\"complet\":true},\"escalade\":false}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private QuartierRepository quartierRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private TourneeRepository tourneeRepository;

    @Autowired
    private ArretTourneeRepository arretRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private EvenementTraiteRepository evenementTraiteRepository;

    @Autowired
    private SimulateurMessageStore messageStore;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private LlmPort llmPort;

    @MockitoBean
    private RoutingPort routingPort;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void preparerLeScenario() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        nettoyerLesDonneesDuScenario();
        creerLesFixtures();
    }

    @Test
    @DisplayName("une conversation WhatsApp cree le client, la commande puis la tournee du jour")
    void une_conversation_cree_le_client_la_commande_puis_la_tournee() throws Exception {
        when(llmPort.generer(anyString(), anyList()))
                .thenReturn(REPONSE_QUESTION)
                .thenReturn(REPONSE_COMMANDE);
        // E1 : question commerciale, aucune commande creee.
        envoyerWebhookChatwoot(EVENEMENT_1, QUESTION);
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(messageStore.messages(CONVERSATION_ID))
                        .hasSize(1));
        assertThat(commandeRepository.findByDateLivraisonSouhaitee(DATE_LIVRAISON)).isEmpty();

        // E2 : commande explicite (produit, quantite, quartier, date).
        envoyerWebhookChatwoot(EVENEMENT_2, MESSAGE_COMMANDE);
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(commandeRepository
                        .findByClientId(identifiantDuClient())).hasSize(1));

        // E3 : la reponse de l'IA a bien ete envoyee au client (mode LOCAL du simulateur).
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(messageStore.messages(CONVERSATION_ID))
                        .hasSize(2));
        assertThat(conversationRepository.findByChatwootConversationId(CONVERSATION_ID))
                .get()
                .extracting(Conversation::getMode)
                .isEqualTo(ModeConversation.IA);

        // E4 : le client a ete cree et rattache au quartier reconnu.
        final Client client = clientRepository.findByTelephone(TELEPHONE).orElseThrow();
        final Quartier quartier = quartierRepository.findByNom(NOM_QUARTIER).orElseThrow();
        assertThat(client.getNom()).isEqualTo(NOM_CLIENT);
        assertThat(clientRepository.findByQuartierId(quartier.getId()))
                .extracting(Client::getId)
                .contains(client.getId());

        // E5 : la commande respecte le catalogue et la date demandee.
        final UUID commandeId = verifierLaCommande(client.getId());

        // E6 : calcul de la tournee du jour de livraison via l'ecran « Tournee ».
        when(routingPort.optimiser(any()))
                .thenThrow(new IllegalStateException("RouteXL indisponible (scenario)"));
        mockMvc.perform(post("/tournee/calculer")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("date", DATE_LIVRAISON.toString()))
                .andExpect(status().is3xxRedirection());

        final Tournee tournee = tourneeRepository.findByDate(DATE_LIVRAISON).orElseThrow();
        assertThat(tournee.getStatut()).isEqualTo(StatutTournee.VALIDEE);
        final List<ArretTournee> arrets =
                arretRepository.findByTourneeIdOrderByPositionAsc(tournee.getId());
        assertThat(arrets).hasSize(1);
        final ArretTournee arret = arrets.getFirst();
        assertThat(arret.getPosition()).isZero();
        assertThat(arret.getStatut()).isEqualTo(StatutArret.A_LIVRER);
        assertThat(arret.getHeureEstimee()).isEqualTo(HEURE_ARRIVEE_ATTENDUE);
        assertThat(arret.getCommande().getId()).isEqualTo(commandeId);

        // E7 : la commande est passee PLANIFIEE et l'ecran affiche l'arret a 08:02.
        assertThat(commandeRepository.findByDateLivraisonSouhaitee(DATE_LIVRAISON))
                .extracting(Commande::getStatut)
                .containsExactly(StatutCommande.PLANIFIEE);
        mockMvc.perform(get("/tournee")
                        .with(user("admin").roles("ADMIN"))
                        .param("date", DATE_LIVRAISON.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(NOM_CLIENT)))
                .andExpect(content().string(Matchers.containsString("08:02")));
    }

    @Test
    @DisplayName("un webhook rejoue ne cree pas de doublon de commande")
    void un_webhook_rejoue_ne_cree_pas_de_doublon() throws Exception {
        when(llmPort.generer(anyString(), anyList())).thenReturn(REPONSE_COMMANDE);
        envoyerWebhookChatwoot(EVENEMENT_2, MESSAGE_COMMANDE);
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(commandeRepository
                        .findByClientId(identifiantDuClient())).hasSize(1));

        envoyerWebhookChatwoot(EVENEMENT_2, MESSAGE_COMMANDE);

        assertThat(commandeRepository.findByClientId(identifiantDuClient())).hasSize(1);
        assertThat(messageStore.messages(CONVERSATION_ID)).hasSize(1);
        assertThat(evenementTraiteRepository.findById(EVENEMENT_2)).isPresent();
    }

    /**
     * Verifie la commande creee par l'IA et retourne son identifiant. Les lignes etant chargees
     * paresseusement, la lecture se fait dans une transaction explicite.
     */
    private UUID verifierLaCommande(final UUID clientId) {
        return transactionTemplate.execute(ignored -> {
            final List<Commande> commandes = commandeRepository.findByClientId(clientId);
            assertThat(commandes).hasSize(1);
            final Commande commande = commandes.getFirst();
            assertThat(commande.getStatut()).isEqualTo(StatutCommande.A_PLANIFIER);
            assertThat(commande.getSource()).isEqualTo(SourceCommande.WHATSAPP_IA);
            assertThat(commande.getModePaiement()).isEqualTo(ModePaiement.ESPECES);
            assertThat(commande.getStatutPaiement()).isEqualTo(StatutPaiement.NON_PAYE);
            assertThat(commande.getMontantTotal()).isEqualTo(3L * PRIX_KG);
            assertThat(commande.getMontantPaye()).isZero();
            assertThat(commande.getDateLivraisonSouhaitee()).isEqualTo(DATE_LIVRAISON);
            assertThat(commande.getNotes()).isNull();
            assertThat(commande.getLignes()).hasSize(1);
            assertThat(commande.getLignes().getFirst().getQuantite()).isEqualTo(3);
            assertThat(commande.getLignes().getFirst().getPrixUnitaire()).isEqualTo(PRIX_KG);
            assertThat(commande.getLignes().getFirst().getSousTotal()).isEqualTo(3L * PRIX_KG);
            return commande.getId();
        });
    }

    private UUID identifiantDuClient() {
        return clientRepository.findByTelephone(TELEPHONE).orElseThrow().getId();
    }

    private void creerLesFixtures() {
        quartierRepository.findByNom(NOM_QUARTIER)
                .orElseGet(() -> quartierRepository.save(new Quartier(NOM_QUARTIER, "Centre",
                        LATITUDE_QUARTIER, LONGITUDE_QUARTIER, 0, true)));
        produitRepository.findByNomIgnoreCase(NOM_PRODUIT)
                .orElseGet(() -> produitRepository.save(
                        new Produit(NOM_PRODUIT, PRIX_KG, "Kg", true, "")));
    }

    private void nettoyerLesDonneesDuScenario() {
        tourneeRepository.findByDate(DATE_LIVRAISON).ifPresent(tournee -> {
            arretRepository.deleteAll(
                    arretRepository.findByTourneeIdOrderByPositionAsc(tournee.getId()));
            tourneeRepository.delete(tournee);
        });
        clientRepository.findByTelephone(TELEPHONE).ifPresent(client -> {
            commandeRepository.deleteAll(commandeRepository.findByClientId(client.getId()));
            clientRepository.delete(client);
        });
        conversationRepository.findByChatwootConversationId(CONVERSATION_ID)
                .ifPresent(conversationRepository::delete);
        evenementTraiteRepository.deleteAllById(List.of(EVENEMENT_1, EVENEMENT_2));
        messageStore.effacer(CONVERSATION_ID);
    }

    private void envoyerWebhookChatwoot(final String evenement, final String message)
            throws Exception {
        final String corps = "{\"event\":\"message_created\",\"id\":\"" + evenement + "\","
                + "\"message_type\":\"incoming\",\"private\":false,"
                + "\"content\":\"" + message + "\","
                + "\"conversation\":{\"id\":\"" + CONVERSATION_ID + "\"},"
                + "\"sender\":{\"type\":\"contact\",\"phone_number\":\"" + TELEPHONE + "\","
                + "\"name\":\"" + NOM_CLIENT + "\"}}";
        final String horodatage = String.valueOf(Instant.now().getEpochSecond());
        mockMvc.perform(post("/webhooks/chatwoot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Chatwoot-Signature", signatureChatwoot(corps, horodatage))
                        .header("X-Chatwoot-Timestamp", horodatage)
                        .content(corps.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk());
    }

    private String signatureChatwoot(final String corps, final String horodatage) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            final byte[] digest = mac.doFinal((horodatage + "." + corps)
                    .getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(digest);
        } catch (final Exception e) {
            throw new IllegalStateException("Signature HMAC impossible", e);
        }
    }
}

