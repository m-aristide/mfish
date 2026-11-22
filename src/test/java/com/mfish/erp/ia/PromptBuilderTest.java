package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mfish.erp.connaissance.ConnaissanceService;
import com.mfish.erp.conversation.EtatCommande;
import com.mfish.erp.conversation.LigneEtatCommande;
import com.mfish.erp.conversation.StatutEtatCommande;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.quartier.Quartier;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptBuilderTest {

    private static final String BASE = "# Base de connaissances\n- Identite : MFISH\n";

    @Mock
    private ConnaissanceService connaissanceService;

    private PromptBuilder promptBuilder(final String base) {
        when(connaissanceService.contenuCourant()).thenReturn(base);
        return new PromptBuilder(connaissanceService);
    }

    @Test
    @DisplayName("doit_contenir_catalogue_quartiers_et_format_json")
    void doit_contenir_catalogue_quartiers_et_format_json() {
        final String prompt = promptBuilder(BASE).construire(
                List.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")),
                List.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)),
                List.of(LocalDate.of(2026, 11, 1)));

        assertThat(prompt).contains("Attieke", "1000", "Cocody", "escalade", "dateSouhaitee");
    }

    @Test
    @DisplayName("doit_injecter_la_base_de_connaissance_comme_source_unique")
    void doit_injecter_la_base_de_connaissance_comme_source_unique() {
        final String prompt = promptBuilder(BASE).construire(List.of(), List.of(), List.of());

        assertThat(prompt).startsWith(BASE);
        assertThat(prompt).doesNotContain("Tu es l'assistant commercial WhatsApp");
        assertThat(prompt).doesNotContain("Frais de livraison : 1 000 FCFA");
    }

    @Test
    @DisplayName("doit_limiter_le_contexte_aux_n_derniers_messages")
    void doit_limiter_le_contexte_aux_n_derniers_messages() {
        final List<String> messages = IntStream.range(0, 15)
                .mapToObj(i -> "message-" + i).toList();

        final List<String> limite = new PromptBuilder(connaissanceService).limiterContexte(messages);

        assertThat(limite).hasSize(PromptBuilder.MAX_MESSAGES_CONTEXTE);
        assertThat(limite.get(0)).isEqualTo("message-5");
        assertThat(limite.get(limite.size() - 1)).isEqualTo("message-14");
    }

    @Test
    @DisplayName("doit_conserver_le_contexte_si_inferieur_a_la_limite")
    void doit_conserver_le_contexte_si_inferieur_a_la_limite() {
        final List<String> messages = List.of("a", "b", "c");

        assertThat(new PromptBuilder(connaissanceService).limiterContexte(messages))
                .containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("doit_considerer_l_adresse_optionnelle_pour_la_completude")
    void doit_considerer_l_adresse_optionnelle_pour_la_completude() {
        final String prompt = promptBuilder(BASE).construire(
                List.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")),
                List.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)),
                List.of(LocalDate.of(2026, 11, 1)));

        assertThat(prompt).contains("L'adresse precise est optionnelle", "complet a true");
    }

    @Test
    @DisplayName("doit_injecter_l_etat_de_commande_et_les_informations_manquantes")
    void doit_injecter_l_etat_de_commande_et_les_informations_manquantes() {
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null, null, null,
                StatutEtatCommande.EN_COURS);

        final String prompt = promptBuilder(BASE).construire(
                List.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")),
                List.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)),
                List.of(LocalDate.of(2026, 11, 1)), etat);

        assertThat(prompt)
                .contains("Etat de la commande en cours")
                .contains("\"quantite\":3")
                .contains("3 kg de Attieke")
                .contains("Informations encore manquantes : la date de livraison")
                .contains("Ne redemande JAMAIS une information deja presente")
                .doesNotContain("Informations encore manquantes : le produit");
    }

    @Test
    @DisplayName("doit_indiquer_qu_aucune_information_ne_manque_quand_l_etat_est_complet")
    void doit_indiquer_qu_aucune_information_ne_manque_quand_l_etat_est_complet() {
        final EtatCommande etat = new EtatCommande(
                List.of(new LigneEtatCommande("Attieke", 3)), "Cocody", null,
                LocalDate.of(2026, 11, 1), "12h", StatutEtatCommande.EN_COURS);

        final String prompt = promptBuilder(BASE).construire(List.of(), List.of(),
                List.of(LocalDate.of(2026, 11, 1)), etat);

        assertThat(prompt).contains("Informations encore manquantes : aucune")
                .contains("Informations deja collectees : 3 kg de Attieke, quartier Cocody, "
                        + "livraison le 01/11/2026, vers 12h");
    }

    @Test
    @DisplayName("doit_tolerer_un_etat_null")
    void doit_tolerer_un_etat_null() {
        final String prompt = promptBuilder(BASE).construire(List.of(), List.of(),
                List.of(LocalDate.of(2026, 11, 1)), null);

        assertThat(prompt).contains("Informations deja collectees : aucune")
                .contains("Informations encore manquantes : la quantite, le quartier, "
                        + "la date de livraison");
    }
}
