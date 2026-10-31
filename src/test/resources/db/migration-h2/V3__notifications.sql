-- =============================================================================
-- V3 (H2) : notifications, equivalent a la migration PostgreSQL.
-- =============================================================================

CREATE TABLE notifications (
    id          uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    commande_id uuid         NOT NULL REFERENCES commandes(id) ON DELETE CASCADE,
    type        varchar(64)  NOT NULL,
    contenu     varchar(2000),
    statut      varchar(32)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_notification_commande_type UNIQUE (commande_id, type)
);

CREATE INDEX idx_notifications_commande ON notifications(commande_id);
