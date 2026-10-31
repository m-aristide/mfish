-- =============================================================================
-- V4 : comptes d'acces a l'interface d'administration (roles ADMIN/LIVREUR).
-- =============================================================================

CREATE TABLE utilisateurs (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username      varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role          varchar(32)  NOT NULL,
    actif         boolean      NOT NULL DEFAULT true,
    CONSTRAINT chk_utilisateur_role CHECK (role IN ('ADMIN','LIVREUR'))
);
