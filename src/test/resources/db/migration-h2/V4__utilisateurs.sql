-- =============================================================================
-- V4 (H2) : utilisateurs, equivalent a la migration PostgreSQL.
-- =============================================================================

CREATE TABLE utilisateurs (
    id            uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    username      varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role          varchar(32)  NOT NULL,
    actif         boolean      NOT NULL DEFAULT true,
    CONSTRAINT chk_utilisateur_role CHECK (role IN ('ADMIN','LIVREUR'))
);
