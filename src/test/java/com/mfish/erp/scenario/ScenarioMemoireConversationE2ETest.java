package com.mfish.erp.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.Commande;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.conversation.Conversation;
import com.mfish.erp.conversation.ConversationRepository;
import com.mfish.erp.ia.LlmPort;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import com.mfish.erp.simulateur.SimulateurMessageStore;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
 * Scenario de bout en bout de la memoire de conversation WhatsApp (webhook Chatwoot signe).
 *
 * <p>Le LLM est remplace par un stub qui repond en fonction du dernier message du client : les
 * messages fragmentes (« je veux 3 kilos », « Obobogo », « le 19/11/2026 ») sont ainsi rejoues de
 * facon deterministe. On verifie que l'IA ne redemande jamais une information deja fournie, que
 * les corrections remplacent les valeurs precedentes, et que deux conversations n'echangent pas
 * leur etat.
 */
@SpringBootTest(properties = {
        "app.simulateur.active=true",
        "app.simulateur.mode=LOCAL"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScenarioMemoireConversationE2ETest {

    private static final String SECRET = "test-webhook-secret";
    private static final String CONVERSATION_A = "conversation-memoire-a";
    private static final String CONVERSATION_B = "conversation-memoire-b";
    private static final String TELEPHONE_A = "+237690445566";
    private static final String TELEPHONE_B = "+237690778899";
    private static final String NOM_CLIENT_A = "Awa Ndongo";
    private static final String NOM_CLIENT_B = "Bilal Sow";
    private static final String NOM_QUARTIER = "Obobogo";
    private static final String NOM_PRODUIT = "Poisson silure frais";
    private static final long PRIX_KG = 2500L;
    private static final LocalDate DATE_LIVRAISON = LocalDate.of(2026, 11, 20);

    private static final String MESSAGE_3_KG = "Je veux 3 kilos de " + NOM_PRODUIT;
    private static final String MESSAGE_2_KG = "Je veux 2 kilos de " + NOM_PRODUIT;
    private static final String MESSAGE_QUARTIER = NOM_QUARTIER;
    private static final String MESSAGE_DATE = "le 20/11/2026";
    private static final String MESSAGE_CORRECTION = "finalement 5 kilos";

    private static final String JSON_TROIS_KG = commande("3", "null", "null", "commande");
    private static final String JSON_DEUX_KG = commande("2", "null", "null", "commande");
    private static final String JSON_CINQ_KG = commande("5", "null", "null", "modification");
    private static final String JSON_QUARTIER =
            commande("0", "\"Obobogo\"", "null", "commande");
    private static final String JSON_DATE =
            commande("0", "null", "\"2026-11-20\"", "commande");
    private static final String JSON_AUTRE = "{\"reponse\":\"Je vous ecoute.\","
            + "\"intention\":\"autre\",\"commande\":null,\"escalade\":false}";

    private static final Map<String, String> REPONSES = Map.of(
            MESSAGE_3_KG, JSON_TROIS_KG,
            MESSAGE_2_KG, JSON_DEUX_KG,
            MESSAGE_QUARTIER, JSON_QUARTIER,
            MESSAGE_DATE, JSON_DATE,
            MESSAGE_CORRECTION, JSON_CINQ_KG);

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
    private ConversationRepository conversationRepository;

    @Autowired
    private SimulateurMessageStore messageStore;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private LlmPort llmPort;

    private TransactionTemplate transactionTemplate;

    private static String commande(final String quantite, final String quartier,
                                   final String date, final String intention) {
        return "{\"reponse\":\"Bien recu.\",\"intention\":\"" + intention + "\","
                + "\"commande\":{\"produits\":[{\"nom\":\"" + NOM_PRODUIT + "\",\"quantite\":"
                + quantite + "}],\"quartier\":" + quartier + ",\"adresse\":null,"
                + "\"dateSouhaitee\":" + date + ",\"complet\":false},\"escalade\":false}";
    }

    @BeforeEach
    void preparerLeScenario() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        nettoyer();
        creerLesFixtures();
        when(llmPort.generer(anyString(), anyList())).thenAnswer(invocation -> {
            final List<String> contexte = invocation.getArgument(1, List.class);
            final String dernier = contexte.isEmpty() ? "" : contexte.getLast();
            return REPONSES.getOrDefault(dernier, JSON_AUTRE);
        });
    }

    @Test
    @DisplayName("une commande fragmentee ne redemande pas les informations deja fournies")
    void une_commande_fragmentee_ne_redemande_pas_les_informations_deja_fournies() throws Exception {
        envoyer(CONVERSATION_A, TELEPHONE_A, NOM_CLIENT_A, MESSAGE_3_KG);
        final String premiereReponse = attendreReponse(CONVERSATION_A, 1);
        assertThat(premiereReponse)
                .contains("3 kg de " + NOM_PRODUIT)
                .contains("le quartier")
                .doesNotContain("le produit");

        envoyer(CONVERSATION_A, TELEPHONE_A, NOM_CLIENT_A, MESSAGE_QUARTIER);
        final String deuxiemeReponse = attendreReponse(CONVERSATION_A, 2);
        assertThat(deuxiemeReponse)
                .contains("quartier " + NOM_QUARTIER)
                .contains("la date de livraison")
                .doesNotContain("le quartier,");

        // L'etat est bien persiste en base entre deux messages (redemarrage sans perte).
        final Conversation conversation = conversationRepository
                .findByChatwootConversationId(CONVERSATION_A).orElseThrow();
        assertThat(conversation.getEtatCommandeJson())
                .contains(NOM_PRODUIT)
                .contains(NOM_QUARTIER)
                .contains("\"statut\":\"EN_COURS\"");
        assertThat(commandeRepository.findByClientId(identifiantDuClient(TELEPHONE_A))).isEmpty();

        envoyer(CONVERSATION_A, TELEPHONE_A, NOM_CLIENT_A, MESSAGE_DATE);
        final ResumeCommande resume = attendreCommande(TELEPHONE_A);

        assertThat(resume.quantite()).isEqualTo(3);
        assertThat(resume.prixUnitaire()).isEqualTo(PRIX_KG);
        assertThat(resume.dateLivraison()).isEqualTo(DATE_LIVRAISON);
        final Conversation apresCommande = conversationRepository
                .findByChatwootConversationId(CONVERSATION_A).orElseThrow();
        assertThat(apresCommande.getEtatCommandeJson()).contains("\"statut\":\"CONFIRMEE\"");
    }

    @Test
    @DisplayName("une correction de quantite remplace la valeur precedente")
    void une_correction_de_quantite_remplace_la_valeur_precedente() throws Exception {
        envoyer(CONVERSATION_A, TELEPHONE_A, NOM_CLIENT_A, MESSAGE_3_KG);
        attendreReponse(CONVERSATION_A, 1);
        envoyer(CONVERSATION_A, TELEPHONE_A, NOM_CLIENT_A, MESSAGE_QUARTIER);
        attendreReponse(CONVERSATION_A, 2);

        envoyer(CONVERSATION_A, TELEPHONE_A, NOM_CLIENT_A, MESSAGE_CORRECTION);
        final String reponse = attendreReponse(CONVERSATION_A, 3);
        assertThat(reponse).contains("5 kg de " + NOM_PRODUIT)
                .contains("la date de livraison")
                .doesNotContain("3 kg");

        envoyer(CONVERSATION_A, TELEPHONE_A, NOM_CLIENT_A, MESSAGE_DATE);
        final ResumeCommande resume = attendreCommande(TELEPHONE_A);

        assertThat(resume.quantite()).isEqualTo(5);
    }

    @Test
    @DisplayName("deux conversations ne partagent pas leur etat de commande")
    void deux_conversations_ne_partagent_pas_leur_etat_de_commande() throws Exception {
        envoyer(CONVERSATION_A, TELEPHONE_A, NOM_CLIENT_A, MESSAGE_3_KG);
        attendreReponse(CONVERSATION_A, 1);
        envoyer(CONVERSATION_B, TELEPHONE_B, NOM_CLIENT_B, MESSAGE_2_KG);
        attendreReponse(CONVERSATION_B, 1);
        envoyer(CONVERSATION_B, TELEPHONE_B, NOM_CLIENT_B, MESSAGE_QUARTIER);
        attendreReponse(CONVERSATION_B, 2);
        envoyer(CONVERSATION_B, TELEPHONE_B, NOM_CLIENT_B, MESSAGE_DATE);
        final ResumeCommande resumeB = attendreCommande(TELEPHONE_B);

        assertThat(resumeB.quantite()).isEqualTo(2);
        assertThat(commandeRepository.findByClientId(identifiantDuClient(TELEPHONE_A))).isEmpty();
        final Conversation conversationA = conversationRepository
                .findByChatwootConversationId(CONVERSATION_A).orElseThrow();
        assertThat(conversationA.getEtatCommandeJson())
                .contains("\"quantite\":3")
                .doesNotContain(NOM_QUARTIER);
    }

    private void envoyer(final String conversationId, final String telephone, final String nom,
                         final String message) throws Exception {
        final String corps = "{\"event\":\"message_created\",\"id\":\"evt-" + UUID.randomUUID()
                + "\",\"message_type\":\"incoming\",\"private\":false,"
                + "\"content\":\"" + message + "\","
                + "\"conversation\":{\"id\":\"" + conversationId + "\"},"
                + "\"sender\":{\"type\":\"contact\",\"phone_number\":\"" + telephone + "\","
                + "\"name\":\"" + nom + "\"}}";
        final String horodatage = String.valueOf(Instant.now().getEpochSecond());
        mockMvc.perform(post("/webhooks/chatwoot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Chatwoot-Signature", signatureChatwoot(corps, horodatage))
                        .header("X-Chatwoot-Timestamp", horodatage)
                        .content(corps.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk());
    }

    private String attendreReponse(final String conversationId, final int nombre) {
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(messageStore.messages(conversationId))
                        .hasSize(nombre));
        return messageStore.messages(conversationId).getLast().contenu();
    }

    private ResumeCommande attendreCommande(final String telephone) {
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(commandeRepository
                        .findByClientId(identifiantDuClient(telephone))).hasSize(1));
        return transactionTemplate.execute(ignored -> {
            final Commande commande = commandeRepository
                    .findByClientId(identifiantDuClient(telephone)).getFirst();
            final var ligne = commande.getLignes().getFirst();
            return new ResumeCommande(ligne.getQuantite(), ligne.getPrixUnitaire(),
                    commande.getDateLivraisonSouhaitee());
        });
    }

    /** Vue detachee d'une commande : les lignes sont paresseuses et lues dans la transaction. */
    private record ResumeCommande(int quantite, long prixUnitaire, LocalDate dateLivraison) {
    }

    private UUID identifiantDuClient(final String telephone) {
        return clientRepository.findByTelephone(telephone).orElseThrow().getId();
    }

    private void creerLesFixtures() {
        quartierRepository.findByNom(NOM_QUARTIER)
                .orElseGet(() -> quartierRepository.save(new Quartier(NOM_QUARTIER, "Centre",
                        3.8179466, 11.4946833, 0, true)));
        produitRepository.findByNomIgnoreCase(NOM_PRODUIT)
                .orElseGet(() -> produitRepository.save(
                        new Produit(NOM_PRODUIT, PRIX_KG, "Kg", true, "")));
    }

    private void nettoyer() {
        for (final String telephone : List.of(TELEPHONE_A, TELEPHONE_B)) {
            clientRepository.findByTelephone(telephone).ifPresent(client -> {
                commandeRepository.deleteAll(commandeRepository.findByClientId(client.getId()));
                clientRepository.delete(client);
            });
        }
        for (final String conversationId : List.of(CONVERSATION_A, CONVERSATION_B)) {
            conversationRepository.findByChatwootConversationId(conversationId)
                    .ifPresent(conversationRepository::delete);
            messageStore.effacer(conversationId);
        }
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
