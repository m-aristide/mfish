-- =============================================================================
-- V1 : schema initial de l'application de gestion de livraison.
-- PostgreSQL 16+ (image pgvector/pgvector:pg16). gen_random_uuid() est natif.
-- Les montants sont des entiers (plus petite unite monetaire). Jamais de double/float.
-- =============================================================================

CREATE TABLE quartiers (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nom           varchar(255)    NOT NULL UNIQUE,
    zone          varchar(255),
    latitude      double precision,
    longitude     double precision,
    ordre_defaut  integer         NOT NULL DEFAULT 0,
    actif         boolean         NOT NULL DEFAULT true
);

CREATE TABLE clients (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nom         varchar(255)   NOT NULL,
    telephone   varchar(32)    NOT NULL UNIQUE,
    quartier_id uuid           REFERENCES quartiers(id),
    adresse     varchar(500),
    repere      varchar(500),
    latitude    double precision,
    longitude   double precision,
    notes       varchar(2000),
    created_at  timestamptz    NOT NULL,
    updated_at  timestamptz    NOT NULL
);

CREATE INDEX idx_clients_quartier ON clients(quartier_id);

CREATE TABLE produits (
    id    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nom   varchar(255) NOT NULL,
    prix  bigint       NOT NULL CHECK (prix >= 0),
    unite varchar(64),
    actif boolean      NOT NULL DEFAULT true
);

CREATE INDEX idx_produits_nom ON produits(nom);

CREATE TABLE commandes (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id                uuid         NOT NULL REFERENCES clients(id),
    statut                   varchar(32)  NOT NULL,
    date_livraison_souhaitee date,
    montant_total            bigint       NOT NULL DEFAULT 0 CHECK (montant_total >= 0),
    mode_paiement            varchar(32)  NOT NULL,
    statut_paiement          varchar(32)  NOT NULL,
    montant_paye             bigint       NOT NULL DEFAULT 0 CHECK (montant_paye >= 0),
    notes                    varchar(2000),
    source                   varchar(32)  NOT NULL,
    created_at               timestamptz  NOT NULL,
    updated_at               timestamptz  NOT NULL,
    version                  bigint       NOT NULL DEFAULT 0,
    CONSTRAINT chk_commande_statut
        CHECK (statut IN ('A_PLANIFIER','PLANIFIEE','EN_COURS','LIVREE','ANNULEE')),
    CONSTRAINT chk_commande_mode_paiement
        CHECK (mode_paiement IN ('ESPECES','MOBILE_MONEY','DEJA_PAYE')),
    CONSTRAINT chk_commande_statut_paiement
        CHECK (statut_paiement IN ('NON_PAYE','PARTIEL','PAYE')),
    CONSTRAINT chk_commande_source
        CHECK (source IN ('WHATSAPP_IA','MANUEL'))
);

CREATE INDEX idx_commandes_client ON commandes(client_id);
CREATE INDEX idx_commandes_statut ON commandes(statut);
-- Index filtre : commandes restant a planifier.
CREATE INDEX idx_commandes_a_planifier
    ON commandes(date_livraison_souhaitee) WHERE statut = 'A_PLANIFIER';

CREATE TABLE lignes_commande (
    id            uuid    PRIMARY KEY DEFAULT gen_random_uuid(),
    commande_id   uuid    NOT NULL REFERENCES commandes(id) ON DELETE CASCADE,
    produit_id    uuid    NOT NULL REFERENCES produits(id),
    quantite      integer NOT NULL CHECK (quantite > 0),
    prix_unitaire bigint  NOT NULL CHECK (prix_unitaire >= 0)
);

CREATE INDEX idx_lignes_commande_commande ON lignes_commande(commande_id);
CREATE INDEX idx_lignes_commande_produit ON lignes_commande(produit_id);

CREATE TABLE tournees (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    date             date           NOT NULL UNIQUE,
    point_depart_lat double precision,
    point_depart_lng double precision,
    heure_depart     time,
    statut           varchar(32)    NOT NULL,
    CONSTRAINT chk_tournee_statut
        CHECK (statut IN ('BROUILLON','VALIDEE','EN_COURS','TERMINEE'))
);

CREATE TABLE arrets_tournee (
    id            uuid    PRIMARY KEY DEFAULT gen_random_uuid(),
    tournee_id    uuid    NOT NULL REFERENCES tournees(id) ON DELETE CASCADE,
    commande_id   uuid    NOT NULL REFERENCES commandes(id),
    position      integer NOT NULL CHECK (position >= 0),
    heure_estimee time,
    heure_reelle  time,
    statut        varchar(32) NOT NULL,
    CONSTRAINT uq_arret_tournee_commande UNIQUE (tournee_id, commande_id),
    CONSTRAINT chk_arret_statut
        CHECK (statut IN ('A_LIVRER','LIVRE','ABSENT','REPORTE'))
);

CREATE INDEX idx_arrets_tournee_tournee ON arrets_tournee(tournee_id);

CREATE TABLE conversations (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    chatwoot_conversation_id varchar(255) NOT NULL UNIQUE,
    client_id                uuid         REFERENCES clients(id),
    mode                     varchar(32)  NOT NULL,
    derniere_activite        timestamptz,
    CONSTRAINT chk_conversation_mode CHECK (mode IN ('IA','HUMAIN'))
);

CREATE INDEX idx_conversations_client ON conversations(client_id);

CREATE TABLE evenements_traites (
    id_evenement varchar(255) PRIMARY KEY,
    traite_le    timestamptz NOT NULL
);

CREATE TABLE parametres (
    cle    varchar(255) PRIMARY KEY,
    valeur varchar(2000)
);

CREATE TABLE evenements_log (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    type       varchar(255) NOT NULL,
    payload    jsonb,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_evenements_log_type ON evenements_log(type);
CREATE INDEX idx_evenements_log_created_at ON evenements_log(created_at);
