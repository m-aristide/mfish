-- =============================================================================
-- V5 : base de connaissance de l'IA (contenu Markdown, historique par version).
-- =============================================================================

CREATE TABLE connaissances (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    contenu    varchar(1000000) NOT NULL,
    created_at timestamptz      NOT NULL
);

CREATE INDEX idx_connaissances_created_at ON connaissances(created_at);
