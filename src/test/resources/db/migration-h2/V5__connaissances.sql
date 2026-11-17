-- =============================================================================
-- V5 (H2) : base de connaissance, equivalent a la migration PostgreSQL.
-- =============================================================================

CREATE TABLE connaissances (
    id         uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    contenu    varchar(1000000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_connaissances_created_at ON connaissances(created_at);
