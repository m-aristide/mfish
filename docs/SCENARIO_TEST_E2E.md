# Scénario de test de bout en bout — commande WhatsApp et tournée

Ce document décrit le scénario de référence qui valide la chaîne complète :

**conversation client WhatsApp → réponse IA → création du client → enregistrement de la
commande → calcul de la tournée du jour de livraison.**

Il existe deux mises en œuvre, complémentaires :

| Mise en œuvre | Fichier | LLM | Base | Usage |
|---|---|---|---|---|
| Test automatisé (CI) | `src/test/java/com/mfish/erp/scenario/ScenarioCommandeTourneeE2ETest.java` | stub `LlmPort` | H2 (migrations `db/migration-h2`) | `./mvnw test`, reproductible, sans réseau |
| Scénario scripté (manuel) | `scripts/scenario-e2e.sh` | DeepSeek réel | PostgreSQL de l'instance | recette avant livraison, sur l'environnement démarré |

---

## 1. Données du scénario

| Élément | Valeur |
|---|---|
| Client | `Awa Ndongo` |
| Téléphone WhatsApp | `+237690112233` (identifiant du client, unique en base) |
| Quartier demandé | `Obobogo` |
| Adresse | `Rue 1.234, maison 12` |
| Produit | `Poisson silure frais`, 2 500 FCFA / kg |
| Quantité | 3 kg (montant attendu : **7 500 FCFA**) |
| Livraison demandée | **19/11/2026 à 12h** |
| Conversation Chatwoot | identifiant du scénario : `999999` (identifiant numérique, requis par `webhook-chatwoot.sh`) |

### Conversation rejouée

1. **Client** — « Bonjour, est-ce que vous avez du poisson silure frais ? »
2. **IA** — réponse commerciale, aucune commande créée (`intention = question`).
3. **Client** — « Je prends 3 kilos de poisson silure frais, livraison le 19/11/2026 a 12h
   a Obobogo, rue 1.234 maison 12. »
4. **IA** — confirmation, commande créée (`intention = commande`).
5. **Commerçant** — écran « Tournée », date `19/11/2026`, bouton *Calculer / recalculer*.

> ⚠️ Le second message doit être **autonome** (produit + quantité + quartier + date **et adresse**
> dans le même message) : l'historique de conversation n'est pas encore stocké, donc le modèle ne
> reçoit que le message courant. Voir §8.3.

---

## 2. Prérequis

### 2.1 Test automatisé

- JDK 25 (`JAVA_HOME` positionné : le wrapper échoue sinon avec
  `release version 25 not supported`).
- Aucun conteneur Docker : le profil `test` utilise H2 (`src/test/resources/application-test.yaml`).
- Aucun secret réel : `application-test.yaml` fournit des valeurs factices.

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-25.0.5.0.3"   # Windows : $env:JAVA_HOME=...
./mvnw -Dtest=ScenarioCommandeTourneeE2ETest test
```

### 2.2 Scénario scripté

- Application démarrée (`scripts/start.sh`) et `/actuator/health` à `UP`.
- `DEEPSEEK_API_KEY` valide : **l'IA est réellement appelée**.
- `CHATWOOT_WEBHOOK_SECRET` identique à celui de l'application (lu dans `.env.local` par
  `scripts/webhook-chatwoot.sh`).
- PostgreSQL accessible : `docker exec mfish-postgres-1 psql` (`DB_CONTAINER`, `DB_USER`,
  `DB_NAME` surchargeables).
- Compte administrateur : `ADMIN_USER` / `ADMIN_PASSWORD` (défaut `admin` / `admin123`).
- Référentiel : le produit `Poisson silure frais` doit exister et être actif. Le quartier
  `Obobogo` doit exister (voir §8.1 : le seed de production ne contient que `Obobogo Rail`).

```bash
scripts/scenario-e2e.sh                 # date 2026-11-19
scripts/scenario-e2e.sh 2026-11-20      # autre date de livraison
```

Le script commence par supprimer les données d'une exécution précédente (client, commandes,
arrêts, tournée du jour, conversation) afin d'être rejouable, puis vérifie les critères
A1 → A7 en base. Il se termine par un code de sortie non nul en cas d'échec.

---

## 3. Déroulé nominal

| Étape | Action | Composant | Résultat attendu |
|---|---|---|---|
| **É0** | `GET /actuator/health` | Actuator | `200`, statut `UP` |
| **É1** | `POST /webhooks/chatwoot` (message 1, signé) | `ChatwootWebhookController` → `SignatureWebhookService` → `WebhookService` → `TraitementMessageService` → `OrchestrateurIa` | `200` ; événement enregistré dans `evenements_traites` ; réponse IA envoyée ; **aucune commande** |
| **É2** | `POST /webhooks/chatwoot` (message 2, signé) | idem | `200` ; client créé (`+237690112233`), rattaché au quartier, commande créée |
| **É3** | Lecture base | `clients`, `commandes`, `lignes_commande` | voir **A1 → A5** |
| **É4** | `POST /tournee/calculer?date=2026-11-19` (session administrateur) | `TourneeWebController` → `TourneeService.calculer(date)` | redirection `302` ; tournée `VALIDEE` |
| **É5** | Lecture base | `tournees`, `arrets_tournee`, `commandes` | voir **A6 → A7** |
| **É6** | `GET /tournee?date=2026-11-19` | `tournee/liste.html` | `200` ; l'arrêt affiche le client et `08:02` |

### Détail des effets attendus

**É1 — question** : `intention = question`, `commande = null` →
`OrchestrateurIa` renvoie la réponse sans rien persister. La conversation passe à
`derniere_activite = now` (mode `IA`).

**É2 — commande** : l'IA renvoie
```json
{"reponse":"...","intention":"commande",
 "commande":{"produits":[{"nom":"poisson silure frais","quantite":3}],
             "quartier":"Obobogo","adresse":"Rue 1.234, maison 12",
             "dateSouhaitee":"2026-11-19","complet":true},
 "escalade":false}
```
`OrchestrateurIa` rapproche le produit du catalogue (prix en base), associe le quartier au
client, puis `CommandeService.creer(...)` enregistre la commande en `A_PLANIFIER` avec
`SourceCommande.WHATSAPP_IA`.

**É4/É5 — tournée** : `TourneeService.calculer(date)` sélectionne les commandes
`A_PLANIFIER`/`PLANIFIEE`/`EN_COURS` dont `date_livraison_souhaitee = 2026-11-19`, tente RouteXL,
puis bascule sur le plan B local (`OptimisationLocale`, 25 km/h, 5 min par arrêt, départ 08:00)
si RouteXL échoue. Le point de départ est celui de la tournée
(`3.816110073189516, 11.489151853853572`) ; le quartier `Obobogo` est à
`3.8179466, 11.4946833`, soit **≈ 0,65 km** → `Math.round(0,65 / 25 × 60) = 2 min` →
**arrivée estimée à 08:02**.

> L'heure de livraison demandée par le client (« 12h ») n'est **pas** un critère de calcul : le
> modèle de données ne comporte pas d'heure souhaitée (§8.2). Elle est conservée uniquement dans
> la conversation et dans la réponse de l'IA.

---

## 4. Définition de « terminé » (critères d'acceptation)

| # | Critère | Contrôle |
|---|---|---|
| **A1** | Le webhook signé est accepté (`200`) et l'événement est mémorisé | `SELECT count(*) FROM evenements_traites WHERE id = '<event id>'` = `1` |
| **A2** | Le client est créé, sans doublon de téléphone | `SELECT nom FROM clients WHERE telephone = '+237690112233'` = `Awa Ndongo` |
| **A3** | Le client est rattaché au quartier reconnu et l'adresse est enregistrée | `SELECT q.nom FROM clients cl JOIN quartiers q ON q.id = cl.quartier_id WHERE cl.telephone = '+237690112233'` = `Obobogo` |
| **A4** | La commande est enregistrée avec la bonne date et la bonne source | `statut = A_PLANIFIER`, `date_livraison_souhaitee = 2026-11-19`, `source = WHATSAPP_IA` |
| **A5** | Le montant est celui du **catalogue** (jamais celui proposé par l'IA) | `montant_total = 7500`, `montant_paye = 0`, `statut_paiement = NON_PAYE`, 1 ligne `quantite = 3`, `prix_unitaire = 2500` |
| **A6** | La tournée du 19/11/2026 est calculée et validée | `SELECT statut FROM tournees WHERE date = '2026-11-19'` = `VALIDEE` ; 1 arrêt à `position = 0`, `statut = A_LIVRER` |
| **A7** | L'arrêt porte une heure estimée cohérente et la commande est planifiée | `heure_estimee = 08:02` ; `commandes.statut = PLANIFIEE` ; l'écran `/tournee?date=2026-11-19` affiche le client et `08:02` |

### Requêtes de contrôle

```sql
-- A1 : idempotence du webhook
SELECT id, recu_le FROM evenements_traites ORDER BY recu_le DESC LIMIT 5;

-- A2 / A3 : client et quartier
SELECT cl.nom, cl.telephone, cl.adresse, q.nom AS quartier
  FROM clients cl LEFT JOIN quartiers q ON q.id = cl.quartier_id
 WHERE cl.telephone = '+237690112233';

-- A4 / A5 : commande et lignes
SELECT c.statut, c.date_livraison_souhaitee, c.source, c.montant_total,
       c.montant_paye, c.statut_paiement, c.notes
  FROM commandes c JOIN clients cl ON cl.id = c.client_id
 WHERE cl.telephone = '+237690112233';

SELECT l.quantite, l.prix_unitaire, p.nom
  FROM lignes_commande l
  JOIN commandes c ON c.id = l.commande_id
  JOIN clients cl ON cl.id = c.client_id
  LEFT JOIN produits p ON p.id = l.produit_id
 WHERE cl.telephone = '+237690112233';

-- A6 / A7 : tournée du jour et arrêts
SELECT t.date, t.statut, t.heure_depart
  FROM tournees t WHERE t.date = DATE '2026-11-19';

SELECT a.position, a.statut, a.heure_estimee, cl.nom, c.montant_total
  FROM arrets_tournee a
  JOIN tournees t ON t.id = a.tournee_id
  JOIN commandes c ON c.id = a.commande_id
  JOIN clients cl ON cl.id = c.client_id
 WHERE t.date = DATE '2026-11-19'
 ORDER BY a.position;
```

---

## 5. Test automatisé (`ScenarioCommandeTourneeE2ETest`)

```bash
./mvnw -Dtest=ScenarioCommandeTourneeE2ETest test
```

Le test démarre le contexte Spring complet (profil `test`, base H2) et rejoue le scénario par
les **mêmes points d'entrée HTTP** que la production.

| Élément | Choix | Raison |
|---|---|---|
| LLM | `@MockitoBean LlmPort` renvoyant deux JSON figés | aucune dépendance réseau/coût ; le reste de la chaîne IA (parsing, rapprochement, validation) est réellement exécuté |
| Messagerie | `app.simulateur.active=true`, `app.simulateur.mode=LOCAL` → `SimulateurMessagingPort` | les réponses de l'IA sont stockées en mémoire (`SimulateurMessageStore`) au lieu d'appeler Chatwoot |
| RouteXL | `@MockitoBean RoutingPort` levant une exception | force le plan B local : heures d'arrivée déterministes (`TourneeService.optimiser` capture l'échec) |
| Webhooks | `POST /webhooks/chatwoot` signé (`sha256=HMAC(secret, "<ts>.<corps>")`) | vérifie le filtrage par signature et l'idempotence |
| Tournée | `POST /tournee/calculer?date=...` avec session `ADMIN` + CSRF | passe par `TourneeWebController`, la sécurité et la redirection |
| Attentes asynchrones | Awaitility (20 s max) | le traitement du webhook est exécuté sur le `TaskExecutor` |

Le test crée lui-même ses fixtures (le référentiel H2 ne contient ni les quartiers de Yaoundé ni
le produit du scénario) :

- quartier `Obobogo` — `3.8179466, 11.4946833` (coordonnées du quartier `Obobogo Rail` du seed
  de production), `ordre_defaut = 0` ;
- produit `Poisson silure frais` — 2 500 FCFA / `Kg`.

Il nettoie ensuite systématiquement les données du scénario (arrêts, tournée du jour, commandes,
client, conversation, événements) pour rester rejouable sur la base H2 partagée.

Assertions couvertes : A1 → A7, plus un test d'idempotence (rejeu du même `event id` : aucune
seconde commande, aucune seconde réponse).

---

## 6. Scénario scripté (`scripts/scenario-e2e.sh`)

Le script enchaîne, sur une instance réelle :

1. **pré-vol** : `/actuator/health` = `200`, PostgreSQL joignable ;
2. **remise à zéro** des données du scénario ;
3. **contrôle du référentiel** : produit actif (prix lu en base → montant attendu calculé) et
   quartier existant ;
4. **deux webhooks Chatwoot signés** via `scripts/webhook-chatwoot.sh` (question puis commande) ;
5. **attente** de la création de la commande (90 s max, traitement asynchrone + IA réelle) ;
6. **vérifications** A1 → A7 en SQL ;
7. **calcul de la tournée** : connexion administrateur (cookie + CSRF), puis
   `POST /tournee/calculer?date=...` avec l'en-tête `X-CSRF-TOKEN` (comme `app.js`) ;
8. **bilan** : `$pass` / `$fail`, code de sortie `1` si un contrôle échoue.

Variables principales : `BASE_URL`, `ADMIN_USER`, `ADMIN_PASSWORD`, `TELEPHONE`, `NOM`,
`QUARTIER`, `PRODUIT`, `QUANTITE`, `HEURE_ATTENDUE` (défaut `08:02`, valable uniquement si la
tournée ne contient qu'un arrêt), `DB_CONTAINER` (défaut `mfish-postgres-1`), `DB_USER`,
`DB_NAME`.

> Le script **modifie** la base ciblée (création de client/commande, calcul de tournée) et
> supprime au passage la tournée du jour choisi. Ne pas l'exécuter sur la production sans accord.

---

## 7. Variantes

### 7.1 Conversation visible dans Chatwoot (`APP_SIMULATEUR_MODE=CHATWOOT`)

1. Renseigner `APP_SIMULATEUR_ACTIVE=true`, `APP_SIMULATEUR_MODE=CHATWOOT` et
   `APP_SIMULATEUR_INBOX=<inbox_identifier>` (voir `docs/CHATWOOT_SETUP.md` §4.2).
   Sans inbox, `SimulateurService` échoue (`500`) : c'est le cas du modèle `.env.local` livré,
   où `APP_SIMULATEUR_INBOX` est vide.
2. Ouvrir `http://127.0.0.1:8080/simulateur`, saisir le téléphone `+237690112233`, envoyer les
   deux messages du §1.
3. Les réponses de l'IA apparaissent dans la conversation Chatwoot correspondante ; les
   commandes sont créées de la même façon qu'en mode `LOCAL`.
4. Terminer par l'écran « Tournée » (`/tournee?date=2026-11-19`).

### 7.2 WhatsApp réel

Le même scénario s'applique depuis un téléphone, en substituant `TELEPHONE` par le numéro réel
et en passant par la Cloud API (`docs/WHATSAPP_SETUP.md`). Le webhook devient
`POST /webhooks/whatsapp` (signature `X-Hub-Signature-256`) ; le reste de la chaîne est
identique.

---

## 8. Écarts et limites connus

Ces points sont **constatés dans le code actuel** ; ils n'empêchent pas le scénario nominal mais
conditionnent son interprétation.

### 8.1 Quartier `Obobogo` absent du référentiel de production

`V9__quartiers_yaounde.sql` contient `Obobogo Rail` (`3.8179466, 11.4946833`) mais pas
`Obobogo`. Or `RapprochementQuartiers` applique une **égalité stricte normalisée**, puis un
Levenshtein ≤ 1 : `obobogo` ne correspond pas à `obobogo rail` (distance 5) et la commande est
alors escaladée (`reprendre-main`) sans être enregistrée.

Deux issues possibles :

- créer le quartier `Obobogo` dans le référentiel (immédiat, aucune modification de code) —
  c'est ce que fait le test automatisé ;
- faire évoluer le rapprochement vers une correspondance par **inclusion** (le nom saisi est
  contenu dans le nom du quartier, ou l'inverse) avec un test unitaire dédié.

### 8.2 Heure de livraison non modélisée

`Commande` ne porte que `date_livraison_souhaitee` ; l'heure demandée (« 12h ») n'est ni
extraite par `CommandeIa`, ni stockée, ni utilisée par la tournée. L'arrêt est estimé depuis
l'heure de départ de la tournée (08:00). Pour la prendre en compte : migration
`heure_livraison_souhaitee`, champ `heureSouhaitee` dans `CommandeIa` et dans le prompt,
puis affichage dans `tournee/liste.html`.

### 8.3 Commande sans adresse

Deux problèmes cumulatifs empêchaient la création d'une commande quand le client donnait
produit + quantité + quartier + date mais **pas d'adresse précise** :

1. **Doublon du message** (corrigé) : `TraitementMessageService` et `WhatsAppConversationService`
   transmettaient `List.of(message)` comme historique puis le même message en argument → le
   modèle recevait le message deux fois.
2. **Critère `complet` trop strict** (corrigé) : le prompt ne précisait pas que l'adresse est
   optionnelle. DeepSeek répondait alors (de façon non déterministe) `complet: false` quand
   l'adresse manquait, et `OrchestrateurIa` ignorait la commande.

Correctifs appliqués :

- les deux appelants passent désormais un historique **vide** (`List.of()`) — le message courant
  n'est transmis qu'une fois ;
- le prompt précise que `complet` vaut `true` dès que produit + quantité + quartier + date sont
  connus, l'adresse précise étant optionnelle (`adresse: null`).

L'historique de conversation n'est **pas encore stocké** (mémoire ou colonne dédiée) : chaque
message doit rester autonome.

### 8.4 Lien conversation ↔ client non persisté

`conversations.client_id` et `conversations.telephone` ne sont jamais renseignés par
`TraitementMessageService` : le rapprochement se fait uniquement par téléphone côté client.
Les écrans « Conversations » ne peuvent donc pas afficher le client associé.

### 8.5 Aucune notification après calcul de la tournée

`ServiceNotification` n'a aucun appelant : le client n'est pas informé de son créneau. Le
scénario s'arrête volontairement à la tournée `VALIDEE`.

---

## 9. Tests négatifs à exécuter en complément

| # | Situation | Attendu |
|---|---|---|
| **N1** | `POST /webhooks/chatwoot` sans en-tête de signature | `401`, aucune écriture |
| **N2** | Signature avec horodatage vieux de plus de 5 minutes | `401` (`TOLERANCE_HORODATAGE = 5 min`) |
| **N3** | Corps non JSON | `400` (`WebhookPayloadInvalideException`) |
| **N4** | Quartier inconnu (« Bonamoussadi ») | aucune commande, escalade : conversation `HUMAIN`, étiquette `reprendre-main`, réponse « Je vous mets en relation… » |
| **N5** | Produit inconnu (« caviar ») | aucune commande, escalade (`ProduitInconnuException`) |
| **N6** | Rejeu du même `event id` | aucune nouvelle commande, aucune nouvelle réponse (couvert par le test automatisé) |
| **N7** | Message alors que la conversation est en mode `HUMAIN` | message ignoré (aucune réponse IA) |
| **N8** | Calcul de tournée sur une date sans commande | tournée créée en `BROUILLON`, aucun arrêt |

---

## 10. Nettoyage après un scénario manuel

```sql
DELETE FROM arrets_tournee
 WHERE commande_id IN (SELECT c.id FROM commandes c
                         JOIN clients cl ON cl.id = c.client_id
                        WHERE cl.telephone = '+237690112233')
    OR tournee_id IN (SELECT id FROM tournees WHERE date = DATE '2026-11-19');
DELETE FROM tournees WHERE date = DATE '2026-11-19';
DELETE FROM lignes_commande
 WHERE commande_id IN (SELECT c.id FROM commandes c
                         JOIN clients cl ON cl.id = c.client_id
                        WHERE cl.telephone = '+237690112233');
DELETE FROM commandes
 WHERE client_id IN (SELECT id FROM clients WHERE telephone = '+237690112233');
DELETE FROM clients WHERE telephone = '+237690112233';
DELETE FROM conversations WHERE chatwoot_conversation_id = '999999';
```

`scripts/scenario-e2e.sh` exécute ces mêmes suppressions **avant** chaque exécution.

---

## 11. Hors périmètre de ce scénario

- marquage des arrêts (`LIVRE` / `ABSENT` / `REPORTE`) et encaissement ;
- notifications client (voir §8.5) ;
- statistiques et tableau de bord ;
- optimisation RouteXL réelle (le scénario accepte le plan B local ; en production, RouteXL
  peut renvoyer un ordre et des heures différents).

