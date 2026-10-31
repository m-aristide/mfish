package com.mfish.erp.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParseurWebhookTest {

    private final ParseurWebhook parseur = new ParseurWebhook();

    @Test
    @DisplayName("doit_parser_les_champs_utiles")
    void doit_parser_les_champs_utiles() {
        final String corps = "{\"event\":\"message_created\",\"id\":\"evt-1\",\"message_type\":\"incoming\","
                + "\"private\":false,\"content\":\"2 attieke\","
                + "\"conversation\":{\"id\":123},\"sender\":{\"type\":\"contact\"},"
                + "\"contact\":{\"phone_number\":\"+2250700000001\",\"name\":\"Awa\"}}";

        final WebhookPayload payload = parseur.parser(corps);

        assertThat(payload.event()).isEqualTo("message_created");
        assertThat(payload.id()).isEqualTo("evt-1");
        assertThat(payload.messageType()).isEqualTo("incoming");
        assertThat(payload.prive()).isFalse();
        assertThat(payload.content()).isEqualTo("2 attieke");
        assertThat(payload.conversationId()).isEqualTo("123");
        assertThat(payload.senderType()).isEqualTo("contact");
        assertThat(payload.contactPhone()).isEqualTo("+2250700000001");
    }

    @Test
    @DisplayName("doit_rejeter_un_payload_malforme")
    void doit_rejeter_un_payload_malforme() {
        assertThatThrownBy(() -> parseur.parser("pas-du-json"))
                .isInstanceOf(WebhookPayloadInvalideException.class);
    }
}
