package com.mfish.erp.whatsapp.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhatsAppPayloadParserTest {

    private final WhatsAppPayloadParser parser = new WhatsAppPayloadParser();

    @Test
    @DisplayName("doit_parser_un_message_texte")
    void doit_parser_un_message_texte() {
        final MessageEntrant message = parser.parserMessages(lire("text.json")).get(0);

        assertThat(message.type()).isEqualTo(TypeMessage.TEXTE);
        assertThat(message.texte()).isEqualTo("2 attieke");
        assertThat(message.wamid()).isEqualTo("wamid.text.123");
        assertThat(message.telephone()).isEqualTo("2250700000001");
    }

    @Test
    @DisplayName("doit_parser_les_medias")
    void doit_parser_les_medias() {
        assertThat(parser.parserMessages(lire("image.json")).get(0).mediaId()).isEqualTo("media-image-1");
        assertThat(parser.parserMessages(lire("audio.json")).get(0).mediaId()).isEqualTo("media-audio-1");
        assertThat(parser.parserMessages(lire("document.json")).get(0).mediaId()).isEqualTo("media-doc-1");
    }

    @Test
    @DisplayName("doit_parser_les_types_non_textuels")
    void doit_parser_les_types_non_textuels() {
        assertThat(parser.parserMessages(lire("location.json")).get(0).type()).isEqualTo(TypeMessage.LOCALISATION);
        assertThat(parser.parserMessages(lire("contacts.json")).get(0).type()).isEqualTo(TypeMessage.CONTACT);
        assertThat(parser.parserMessages(lire("button.json")).get(0).type()).isEqualTo(TypeMessage.BOUTON);
        assertThat(parser.parserMessages(lire("reaction.json")).get(0).type()).isEqualTo(TypeMessage.REACTION);
    }

    @Test
    @DisplayName("doit_parser_une_reponse_interactive")
    void doit_parser_une_reponse_interactive() {
        final MessageEntrant message = parser.parserMessages(lire("interactive.json")).get(0);

        assertThat(message.type()).isEqualTo(TypeMessage.REPONSE_INTERACTIVE);
        assertThat(message.texte()).isEqualTo("Suivre ma livraison");
    }

    @Test
    @DisplayName("doit_marquer_les_types_non_supportes")
    void doit_marquer_les_types_non_supportes() {
        assertThat(parser.parserMessages(lire("unsupported.json")).get(0).type())
                .isEqualTo(TypeMessage.NON_SUPPORTE);
    }

    @Test
    @DisplayName("doit_parser_les_notifications_de_statut")
    void doit_parser_les_notifications_de_statut() {
        final List<StatutEntrant> statuts = parser.parserStatuts(lire("status.json"));

        assertThat(statuts).hasSize(1);
        assertThat(statuts.get(0).wamid()).isEqualTo("wamid.out.123");
        assertThat(statuts.get(0).statut()).isEqualTo("delivered");
    }

    @Test
    @DisplayName("doit_lever_une_exception_sur_payload_malforme")
    void doit_lever_une_exception_sur_payload_malforme() {
        assertThatThrownBy(() -> parser.parserMessages("pas-du-json".getBytes()))
                .isInstanceOf(WhatsAppPayloadInvalideException.class);
    }

    private byte[] lire(final String fichier) {
        try (final var in = getClass().getClassLoader().getResourceAsStream("whatsapp/" + fichier)) {
            assertThat(in).as("fichier %s", fichier).isNotNull();
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
