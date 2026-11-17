package com.mfish.erp.simulateur;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulateurConversationStoreTest {

    private final SimulateurConversationStore store = new SimulateurConversationStore();

    @Test
    @DisplayName("retourne vide pour une conversation inconnue")
    void retourne_vide_pour_conversation_inconnue() {
        assertThat(store.trouver("inconnue")).isEmpty();
    }

    @Test
    @DisplayName("enregistre et retrouve la reference Chatwoot")
    void enregistre_et_retrouve_la_reference() {
        final SimulateurConversation reference = new SimulateurConversation("src-1", "42");

        store.enregistrer("sim-1", reference);

        assertThat(store.trouver("sim-1")).contains(reference);
    }

    @Test
    @DisplayName("efface la reference d'une conversation")
    void efface_la_reference() {
        store.enregistrer("sim-1", new SimulateurConversation("src-1", "42"));

        store.effacer("sim-1");

        assertThat(store.trouver("sim-1")).isEmpty();
    }
}
