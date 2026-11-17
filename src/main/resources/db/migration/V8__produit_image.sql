-- =============================================================================
-- V8 : image obligatoire pour chaque produit.
-- =============================================================================

ALTER TABLE produits ADD COLUMN image varchar(500);

UPDATE produits SET image = '/images/default-produit.svg' WHERE image IS NULL;

ALTER TABLE produits ALTER COLUMN image SET NOT NULL;
