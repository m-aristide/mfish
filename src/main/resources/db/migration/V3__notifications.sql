-- =============================================================================
-- V3 : suivi des notifications envoyees aux clients (anti-doublon + statut).
-- =============================================================================

CREATE TABLE notifications (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    commande_id uuid         NOT NULL REFERENCES commandes(id) ON DELETE CASCADE,
    type        varchar(64)  NOT NULL,
    contenu     varchar(2000),
    statut      varchar(32)  NOT NULL,
    created_at  timestamptz  NOT NULL,
    CONSTRAINT uq_notification_commande_type UNIQUE (commande_id, type)
);

CREATE INDEX idx_notifications_commande ON notifications(commande_id);
