-- =============================================================================
-- V6 (H2) : archivage logiciel des clients, equivalent a la migration PostgreSQL.
-- =============================================================================

ALTER TABLE clients ADD COLUMN archive boolean NOT NULL DEFAULT false;
