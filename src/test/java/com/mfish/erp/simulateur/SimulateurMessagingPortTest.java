package com.mfish.erp.simulateur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulateurMessagingPortTest {

    private final SimulateurMessageStore store = new SimulateurMessageStore();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneId.of("UTC"));
    private final SimulateurMessagingPort messagingPort = new SimulateurMessagingPort(store, clock);

    @Test
    @DisplayName("enregistre la reponse de l'IA dans le store")
    void enregistre_la_reponse() {
        messagingPort.envoyerMessage("conv-1", "Votre commande est enregistree.");

        assertThat(store.messages("conv-1"))
                .extracting(SimulateurMessage::contenu)
                .containsExactly("Votre commande est enregistree.");
    }

    @Test
    @DisplayName("ignore l'etiquetage sans erreur")
    void ignore_etiquetage_sans_erreur() {
        assertThatCode(() -> messagingPort.etiqueterConversation("conv-1", "reprendre-main"))
                .doesNotThrowAnyException();
    }
}
