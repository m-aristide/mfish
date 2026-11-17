-- =============================================================================
-- V7 : integration officielle WhatsApp Cloud API (Meta).
-- - conversations : identifiant Chatwoot nullable + telephone (E.164) unique
-- - clients : indicateur d'opt-out (contactable = false)
-- - whatsapp_messages : historique local + idempotence (wamid unique) + statuts
-- =============================================================================

ALTER TABLE conversations ALTER COLUMN chatwoot_conversation_id DROP NOT NULL;
ALTER TABLE conversations ADD COLUMN telephone varchar(32);
CREATE UNIQUE INDEX uq_conversations_telephone ON conversations(telephone);

ALTER TABLE clients ADD COLUMN contactable boolean NOT NULL DEFAULT true;

CREATE TABLE whatsapp_messages (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    wamid        varchar(255),
    direction    varchar(16)  NOT NULL,
    telephone    varchar(32)  NOT NULL,
    type         varchar(32)  NOT NULL,
    contenu      text,
    statut       varchar(32)  NOT NULL,
    media_id     varchar(255),
    template_nom varchar(255),
    created_at   timestamptz  NOT NULL
);

CREATE UNIQUE INDEX uq_whatsapp_messages_wamid ON whatsapp_messages(wamid);
CREATE INDEX idx_whatsapp_messages_telephone ON whatsapp_messages(telephone);
CREATE INDEX idx_whatsapp_messages_statut ON whatsapp_messages(statut);
