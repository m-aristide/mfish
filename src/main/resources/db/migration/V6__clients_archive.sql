-- =============================================================================
-- V6 : archivage logiciel des clients (aucune suppression physique).
-- =============================================================================

ALTER TABLE clients ADD COLUMN archive boolean NOT NULL DEFAULT false;
