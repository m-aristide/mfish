-- =============================================================================
-- V10 : memoire de commande par conversation.
-- order_state stocke l'etat structure de la commande en cours (JSON) afin que
-- l'IA ne redemande jamais une information deja fournie, y compris apres un
-- redemarrage de l'application.
-- Type text (et non jsonb) : meme colonne portable PostgreSQL / H2 et etat
-- tolerant (un contenu illisible degrade vers une commande vide).
-- =============================================================================

ALTER TABLE conversations ADD COLUMN order_state text;
