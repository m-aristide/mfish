package com.mfish.erp.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistreMessagesEmisTest {

    private static final Instant DEPART = Instant.parse("2026-11-19T03:42:24Z");

    private HorlogeMobile horloge;
    private RegistreMessagesEmis registre;

    @BeforeEach
    void setUp() {
        horloge = new HorlogeMobile(DEPART);
        registre = new RegistreMessagesEmis(horloge);
    }

    @Test
    @DisplayName("doit_reconnaitre_le_message_emis_une_seule_fois")
    void doit_reconnaitre_le_message_emis_une_seule_fois() {
        registre.enregistrer("11", "Oui, nous avons du silure frais.");

        assertThat(registre.consommerEcho("11", "Oui, nous avons du silure frais.")).isTrue();
        assertThat(registre.consommerEcho("11", "Oui, nous avons du silure frais.")).isFalse();
    }

    @Test
    @DisplayName("doit_ignorer_les_espaces_superflus")
    void doit_ignorer_les_espaces_superflus() {
        registre.enregistrer("11", "Merci de preciser le lieu de livraison");

        assertThat(registre.consommerEcho("11", "\n Merci de preciser  le lieu de livraison \n"))
                .isTrue();
    }

    @Test
    @DisplayName("doit_distinguer_les_conversations")
    void doit_distinguer_les_conversations() {
        registre.enregistrer("11", "Bonjour");

        assertThat(registre.consommerEcho("12", "Bonjour")).isFalse();
        assertThat(registre.consommerEcho("11", "Bonjour")).isTrue();
    }

    @Test
    @DisplayName("doit_perimer_les_entrees_anciennes")
    void doit_perimer_les_entrees_anciennes() {
        registre.enregistrer("11", "Bonjour");

        horloge.avancer(Duration.ofMinutes(11));

        assertThat(registre.consommerEcho("11", "Bonjour")).isFalse();
    }

    @Test
    @DisplayName("doit_oublier_un_message_dont_l_envoi_a_echoue")
    void doit_oublier_un_message_dont_l_envoi_a_echoue() {
        registre.enregistrer("11", "Bonjour");
        registre.oublier("11", "Bonjour");

        assertThat(registre.consommerEcho("11", "Bonjour")).isFalse();
    }

    @Test
    @DisplayName("doit_accepter_un_contenu_vide_ou_absent_sans_planter")
    void doit_accepter_un_contenu_vide_ou_absent_sans_planter() {
        registre.enregistrer("11", null);
        registre.enregistrer(null, "Bonjour");

        assertThat(registre.consommerEcho("11", null)).isFalse();
        assertThat(registre.consommerEcho(null, "Bonjour")).isFalse();
    }

    /** Horloge pilotable pour verifier la peremption des entrees. */
    private static final class HorlogeMobile extends Clock {

        private Instant instant;

        private HorlogeMobile(final Instant instant) {
            this.instant = instant;
        }

        private void avancer(final Duration duree) {
            instant = instant.plus(duree);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
