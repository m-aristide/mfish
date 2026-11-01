-- =============================================================================
-- V7 (H2) : equivalent H2 de la migration WhatsApp Cloud API (voir V7 production).
-- =============================================================================

ALTER TABLE conversations ALTER COLUMN chatwoot_conversation_id DROP NOT NULL;
ALTER TABLE conversations ADD COLUMN telephone varchar(32);
CREATE UNIQUE INDEX uq_conversations_telephone ON conversations(telephone);

ALTER TABLE clients ADD COLUMN contactable boolean NOT NULL DEFAULT true;

CREATE TABLE whatsapp_messages (
    id           uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    wamid        varchar(255),
    direction    varchar(16)  NOT NULL,
    telephone    varchar(32)  NOT NULL,
    type         varchar(32)  NOT NULL,
    contenu      CLOB,
    statut       varchar(32)  NOT NULL,
    media_id     varchar(255),
    template_nom varchar(255),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_whatsapp_messages_wamid ON whatsapp_messages(wamid);
CREATE INDEX idx_whatsapp_messages_telephone ON whatsapp_messages(telephone);
CREATE INDEX idx_whatsapp_messages_statut ON whatsapp_messages(statut);
