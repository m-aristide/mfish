-- =============================================================================
-- V2 (H2) : donnees de demonstration, equivalent a la migration PostgreSQL.
-- =============================================================================

INSERT INTO quartiers (nom, zone, latitude, longitude, ordre_defaut, actif) VALUES
    ('Cocody',   'Nord',   5.3549, -3.9790, 1, true),
    ('Plateau',  'Centre', 5.3203, -4.0173, 2, true),
    ('Marcory',  'Sud',    5.2953, -3.9997, 3, true),
    ('Yopougon', 'Ouest',  5.3379, -4.0762, 4, true);

INSERT INTO produits (nom, prix, unite, actif) VALUES
    ('Poulet braise',   5000, 'piece',     true),
    ('Attieke',         1000, 'plat',      true),
    ('Alloco',           500, 'portion',   true),
    ('Poisson braise',  6000, 'piece',     true),
    ('Jus de bissap',   1000, 'bouteille', true);

INSERT INTO clients (nom, telephone, quartier_id, adresse, created_at, updated_at)
SELECT 'Awa Diomande',      '+2250700000001', q.id, 'Rue des Jardins, villa 12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM quartiers q WHERE q.nom = 'Cocody';

INSERT INTO clients (nom, telephone, quartier_id, adresse, created_at, updated_at)
SELECT 'Koffi N Guessan',   '+2250700000002', q.id, 'Boulevard de la Republique', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM quartiers q WHERE q.nom = 'Plateau';

INSERT INTO clients (nom, telephone, quartier_id, adresse, created_at, updated_at)
SELECT 'Mariam Konate',     '+2250700000003', q.id, 'Cite Fairmont, immeuble B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM quartiers q WHERE q.nom = 'Marcory';

INSERT INTO parametres (cle, valeur) VALUES
    ('duree_arret_minutes',   '5'),
    ('vitesse_moyenne_kmh',   '25'),
    ('fenetre_notification',  '30');
