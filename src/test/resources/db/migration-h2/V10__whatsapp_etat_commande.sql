-- =============================================================================
-- V10 (H2) : equivalent H2 de la memoire de commande par conversation.
-- =============================================================================

ALTER TABLE conversations ADD COLUMN order_state text;
