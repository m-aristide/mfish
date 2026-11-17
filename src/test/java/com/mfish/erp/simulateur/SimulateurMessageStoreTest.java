package com.mfish.erp.simulateur;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulateurMessageStoreTest {

    private final SimulateurMessageStore store = new SimulateurMessageStore();

    @Test
    @DisplayName("enregistre et restitue les messages d'une conversation")
    void enregistre_et_restitue_les_messages() {
        final Instant t1 = Instant.parse("2026-11-16T10:00:00Z");
        final Instant t2 = Instant.parse("2026-11-16T10:00:01Z");

        store.enregistrer("conv-1", "bonjour", t1);
        store.enregistrer("conv-1", "reponse", t2);

        final List<SimulateurMessage> messages = store.messages("conv-1");
        assertThat(messages).extracting(SimulateurMessage::contenu)
                .containsExactly("bonjour", "reponse");
    }

    @Test
    @DisplayName("trie les messages par horodatage")
    void trie_les_messages_par_horodatage() {
        final Instant t1 = Instant.parse("2026-11-16T10:00:00Z");
        final Instant t2 = Instant.parse("2026-11-16T10:00:01Z");

        store.enregistrer("conv-1", "reponse", t2);
        store.enregistrer("conv-1", "bonjour", t1);

        assertThat(store.messages("conv-1"))
                .extracting(SimulateurMessage::contenu)
                .containsExactly("bonjour", "reponse");
    }

    @Test
    @DisplayName("retourne une liste vide pour une conversation inconnue")
    void retourne_liste_vide_pour_conversation_inconnue() {
        assertThat(store.messages("inconnue")).isEmpty();
    }

    @Test
    @DisplayName("efface les messages d'une conversation")
    void efface_les_messages() {
        store.enregistrer("conv-1", "bonjour", Instant.parse("2026-11-16T10:00:00Z"));

        store.effacer("conv-1");

        assertThat(store.messages("conv-1")).isEmpty();
    }
}
