package com.mfish.erp.ia;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.produit.Produit;
import com.mfish.erp.quartier.Quartier;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    @DisplayName("doit_contenir_catalogue_quartiers_et_format_json")
    void doit_contenir_catalogue_quartiers_et_format_json() {
        final String prompt = promptBuilder.construire(
                List.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")),
                List.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)),
                List.of(LocalDate.of(2026, 11, 1)));

        assertThat(prompt).contains("Attieke", "1000", "Cocody", "escalade", "dateSouhaitee");
    }

    @Test
    @DisplayName("doit_limiter_le_contexte_aux_n_derniers_messages")
    void doit_limiter_le_contexte_aux_n_derniers_messages() {
        final List<String> messages = IntStream.range(0, 15)
                .mapToObj(i -> "message-" + i).toList();

        final List<String> limite = promptBuilder.limiterContexte(messages);

        assertThat(limite).hasSize(PromptBuilder.MAX_MESSAGES_CONTEXTE);
        assertThat(limite.get(0)).isEqualTo("message-5");
        assertThat(limite.get(limite.size() - 1)).isEqualTo("message-14");
    }

    @Test
    @DisplayName("doit_conserver_le_contexte_si_inferieur_a_la_limite")
    void doit_conserver_le_contexte_si_inferieur_a_la_limite() {
        final List<String> messages = List.of("a", "b", "c");

        assertThat(promptBuilder.limiterContexte(messages)).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("doit_considerer_l_adresse_optionnelle_pour_la_completude")
    void doit_considerer_l_adresse_optionnelle_pour_la_completude() {
        final String prompt = promptBuilder.construire(
                List.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")),
                List.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)),
                List.of(LocalDate.of(2026, 11, 1)));

        assertThat(prompt).contains("L'adresse precise est optionnelle", "complet a true");
    }
}
