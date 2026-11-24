package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commande.CommandeService;
import com.mfish.erp.connaissance.ConnaissanceService;
import com.mfish.erp.conversation.EtatCommande;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import com.mfish.erp.tournee.GeocodingPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Scenarios conversationnels de la mission MFISH : ordre libre des informations, intentions
 * multiples dans un meme message, correction, question de prix avant finalisation et dates
 * relatives. Le LLM est remplace par un stub deterministe : l'accumulation de l'etat et la
 * resolution des dates relatives sont verifiees de facon reelle.
 */
@ExtendWith(MockitoExtension.class)
class ScenariosConversationTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 23);

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

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"),
            ZoneId.of("Africa/Douala"));

    private OrchestrateurIa orchestrateur;

    private Client client;

    @BeforeEach
    void preparer() {
        when(connaissanceService.contenuCourant()).thenReturn("BASE");
        when(produitRepository.findByActifTrueOrderByNomAsc())
                .thenReturn(List.of(new Produit("Poisson silure frais", 2500, "kg", true, "")));
        when(quartierRepository.findByActifTrueOrderByOrdreDefautAsc()).thenReturn(List.of());
        orchestrateur = new OrchestrateurIa(llmPort, new PromptBuilder(connaissanceService),
                new ParseurReponseIa(), new MemoireCommande(clock), new RapprochementProduits(),
                new RapprochementQuartiers(), geocodingPort, produitRepository, quartierRepository,
                clientRepository, commandeService, clock);
        client = new Client("Awa", "+237690000001");
    }

    private static String commande(final int quantite, final String quartier, final String date,
                                   final String intention) {
        return "{\"reponse\":\"\",\"intention\":\"" + intention + "\","
                + "\"commande\":{\"produits\":[{\"nom\":\"Poisson silure frais\",\"quantite\":"
                + quantite + "}],\"quartier\":" + quartier + ",\"adresse\":null,"
                + "\"dateSouhaitee\":" + date + ",\"complet\":false},\"escalade\":false}";
    }

    private static String commandeAvecReponse(final String reponse, final int quantite,
                                              final String quartier) {
        return "{\"reponse\":\"" + reponse + "\",\"intention\":\"commande\","
                + "\"commande\":{\"produits\":[{\"nom\":\"Poisson silure frais\",\"quantite\":"
                + quantite + "}],\"quartier\":\"" + quartier + "\",\"adresse\":null,"
                + "\"dateSouhaitee\":null,\"complet\":false},\"escalade\":false}";
    }

    private static String question(final String reponse) {
        return "{\"reponse\":\"" + reponse + "\",\"intention\":\"question\","
                + "\"commande\":null,\"escalade\":false}";
    }

    @Test
    @DisplayName("test 1 : informations donnees dans le desordre sans perdre le contexte")
    void informations_dans_le_desordre() {
        when(llmPort.generer(anyString(), anyList())).thenAnswer(inv -> {
            final List<String> contexte = inv.getArgument(1, List.class);
            final String dernier = contexte.getLast();
            return switch (dernier) {
                case "Vous avez le poisson ?" ->
                        question("Oui, notre silure frais est disponible a 2 500 FCFA/kg.");
                case "Je suis a Bastos" -> commande(0, "\"Bastos\"", "null", "commande");
                case "Je veux 4 kg" -> commande(4, "null", "null", "commande");
                case "Ca fait combien ?" ->
                        question("4 kg font 10 000 FCFA (2 500 FCFA/kg), plus 1 000 FCFA de "
                                + "livraison : 11 000 FCFA au total.");
                case "Demain" -> commande(0, "null", "null", "commande");
                default -> question("Je vous ecoute.");
            };
        });

        EtatCommande etat = EtatCommande.vide();
        etat = orchestrateur.traiter(client, etat, List.of(), "Vous avez le poisson ?").etat();
        etat = orchestrateur.traiter(client, etat, List.of(), "Je suis a Bastos").etat();
        etat = orchestrateur.traiter(client, etat, List.of(), "Je veux 4 kg").etat();
        final ResultatIa prix = orchestrateur.traiter(client, etat, List.of(), "Ca fait combien ?");
        final ResultatIa date = orchestrateur.traiter(client, prix.etat(), List.of(), "Demain");

        assertThat(prix.reponseAuClient()).contains("11 000 FCFA");
        assertThat(date.etat().lignes().getFirst().quantite()).isEqualTo(4);
        assertThat(date.etat().quartier()).isEqualTo("Bastos");
        assertThat(date.etat().dateLivraison()).isEqualTo(AUJOURDHUI.plusDays(1));
        assertThat(date.etat().estAConfirmer()).isTrue();
    }

    @Test
    @DisplayName("test 2 : plusieurs intentions dans un seul message")
    void plusieurs_intentions_dans_un_seul_message() {
        when(llmPort.generer(anyString(), anyList())).thenReturn(commandeAvecReponse(
                "Pour 5 kg, le total est de 13 500 FCFA 😊 Je note Mvan.", 5, "Mvan"));

        final ResultatIa resultat = orchestrateur.traiter(client, EtatCommande.vide(), List.of(),
                "Je veux 5 kg, je suis a Mvan, et combien ca coute avec livraison ?");

        assertThat(resultat.reponseAuClient()).contains("13 500 FCFA");
        assertThat(resultat.etat().lignes().getFirst().quantite()).isEqualTo(5);
        assertThat(resultat.etat().quartier()).isEqualTo("Mvan");
        assertThat(resultat.etat().champsManquants())
                .containsExactly(EtatCommande.CHAMP_DATE);
    }

    @Test
    @DisplayName("test 3 : une correction remplace la quantite precedente")
    void correction_remplace_la_quantite() {
        when(llmPort.generer(anyString(), anyList())).thenAnswer(inv -> {
            final List<String> contexte = inv.getArgument(1, List.class);
            final String dernier = contexte.getLast();
            return "Finalement 4 kg".equals(dernier)
                    ? commande(4, "null", "null", "modification")
                    : commande(2, "null", "null", "commande");
        });

        final EtatCommande deuxKilos = orchestrateur.traiter(client, EtatCommande.vide(), List.of(),
                "Je veux 2 kg").etat();
        assertThat(deuxKilos.lignes().getFirst().quantite()).isEqualTo(2);

        final ResultatIa corrige = orchestrateur.traiter(client, deuxKilos, List.of(),
                "Finalement 4 kg");
        assertThat(corrige.etat().lignes().getFirst().quantite()).isEqualTo(4);
    }

    @Test
    @DisplayName("test 6 : une information repetee ne provoque pas de nouvel echec")
    void repetition_de_la_date_n_echoue_pas() {
        when(llmPort.generer(anyString(), anyList()))
                .thenReturn(commande(0, "null", "null", "commande"));

        final EtatCommande demain = orchestrateur.traiter(client, EtatCommande.vide(), List.of(),
                "Je veux etre livre demain").etat();
        assertThat(demain.dateLivraison()).isEqualTo(AUJOURDHUI.plusDays(1));

        final ResultatIa repetition = orchestrateur.traiter(client, demain, List.of(),
                "Je veux etre livre demain");

        assertThat(repetition.escalade()).isFalse();
        assertThat(repetition.commandeCreee()).isFalse();
        assertThat(repetition.etat().dateLivraison()).isEqualTo(AUJOURDHUI.plusDays(1));
    }
}
