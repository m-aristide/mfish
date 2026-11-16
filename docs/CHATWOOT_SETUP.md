# CHATWOOT_SETUP — Chatwoot auto-hébergé et test de bout en bout (PC + téléphone)

Guide pas à pas pour démarrer l'application **avec** Chatwoot, brancher le webhook et tester la
chaîne complète, y compris depuis un téléphone connecté au même réseau Wi-Fi que le PC.

## 1. Chaîne technique

```
Téléphone / WhatsApp / widget
        │  message entrant
        ▼
Chatwoot  http://<IP-LAN>:3000      (services Docker "chatwoot" + "chatwoot-worker")
        │  POST /webhooks/chatwoot  (X-Chatwoot-Signature + X-Chatwoot-Timestamp)
        ▼
Application mfish  http://<IP-LAN>:8080
        │  OrchestrateurIa (DeepSeek) puis API REST Chatwoot (api_access_token)
        ▼
Chatwoot → message "outgoing" affiché dans la conversation (dashboard, widget, WhatsApp…)
```

Points à retenir :

- Chatwoot envoie ses webhooks **via Sidekiq** : sans le service `chatwoot-worker`, Chatwoot
  reçoit les messages mais ne notifie jamais l'application.
- La signature envoyée par Chatwoot est
  `sha256=HMAC-SHA256(secret, "<X-Chatwoot-Timestamp>.<corps brut>")`. L'application
  (`SignatureWebhookService`) l'accepte, avec une tolérance de 5 minutes sur l'horodatage.
- L'IA ne répond que si le contact a un `phone_number`. Le payload webhook officiel de Chatwoot
  expose le contact sous `sender` (`sender.phone_number`, `sender.name`) ; `ParseurWebhook` lit
  `sender` avec repli sur `contact` (scripts locaux/tests). Une boîte « Website live chat » sans
  numéro est donc ignorée par `TraitementMessageService`. Pour tester, utiliser une boîte
  **API** (ou une vraie boîte WhatsApp).
- Un seul `CHATWOOT_WEBHOOK_SECRET` partagé : le secret du webhook créé dans Chatwoot est le même
  que celui lu par l'application.

## 2. Prérequis

- Docker Desktop démarré (PostgreSQL `mfish`, Chatwoot, Redis).
- JDK 25 : `C:\Program Files\Java\jdk-25.0.5.0.3` (le `JAVA_HOME` système pointe sur JDK 17).
- Git Bash (scripts `.sh`) et `openssl` (fourni avec Git Bash).
- Ports libres : **3000** (Chatwoot), **8080** (application). Le port **5432** est déjà pris par
  `mfish-postgres-1`.

## 3. Variables à renseigner dans `.env.local`

| Variable | Rôle | Comment l'obtenir |
|---|---|---|
| `CHATWOOT_SECRET_KEY_BASE` | Secret Rails de Chatwoot | `openssl rand -hex 64` |
| `CHATWOOT_WEBHOOK_SECRET` | Secret HMAC partagé webhook | `openssl rand -hex 32` |
| `CHATWOOT_API_TOKEN` | Jeton de l'agent pour répondre | Chatwoot → Profil → *Access Token* |
| `CHATWOOT_ACCOUNT_ID` | Compte Chatwoot | `1` (premier compte créé) |
| `CHATWOOT_BASE_URL` | URL vue **par l'application** | `http://localhost:3000` |
| `CHATWOOT_FRONTEND_URL` | URL vue **par le téléphone/navigateur** | `http://192.168.103.11:3000` |
| `CHATWOOT_POSTGRES_PASSWORD` | Mot de passe de la base Chatwoot | valeur locale |
| `CHATWOOT_REDIS_PASSWORD` | Mot de passe Redis | valeur locale |

> `docker compose` lit `.env` (et non `.env.local`) : toujours lancer Compose avec
> `--env-file .env.local` pour que Chatwoot reçoive ces valeurs.

## 4. Première initialisation de Chatwoot

Tout est automatisé par un seul script :

```bash
bash scripts/start.sh
```

Il démarre `chatwoot-postgres` et `redis`, lance `db:chatwoot_prepare` **uniquement** si la base n'a
jamais été initialisée, puis démarre `chatwoot` + `chatwoot-worker` et attend que l'API réponde.
Relancer le script ne réinitialise rien (idempotent). Arrêt complet : `bash scripts/stop.sh`.

Équivalent manuel (dépannage), commande par commande :

```bash
# 1. Bases + cache
docker compose --env-file .env.local up -d chatwoot-postgres redis

# 2. Schéma de la base Chatwoot (une seule fois, puis après chaque mise à jour d'image)
docker compose --env-file .env.local run --rm chatwoot bundle exec rails db:chatwoot_prepare

# 3. Application Chatwoot + worker Sidekiq (worker indispensable pour les webhooks)
docker compose --env-file .env.local up -d chatwoot chatwoot-worker

# 4. Vérifications
docker compose --env-file .env.local ps
curl -I http://localhost:3000/api     # doit répondre 200
docker compose --env-file .env.local logs -f chatwoot-worker
```

### 4.1 Créer le compte administrateur

1. Ouvrir <http://localhost:3000> : l'écran d'installation demande le premier utilisateur
   (compte super-admin) puis le nom de la société (ex. `mfish`, **account id = 1**).
2. **Profil → Access Token** : copier le jeton dans `CHATWOOT_API_TOKEN`, puis redémarrer
   l'application (le client Chatwoot est construit au démarrage).

### 4.2 Créer une boîte de réception (sans compte Meta)

*Paramètres → Boîtes de réception → Ajouter une boîte → **API***.
Nom libre (ex. `WhatsApp test`). Récupérer l'identifiant de la boîte :

```bash
curl -s -H "api_access_token: $CHATWOOT_API_TOKEN" \
  http://localhost:3000/api/v1/accounts/1/inboxes
```

La réponse contient `inbox_identifier` (chaîne utilisée par l'API publique, section 6.2).

> Avec un vrai compte WhatsApp Cloud API, créer plutôt une boîte **WhatsApp** (voir
> `docs/WHATSAPP_SETUP.md`) : les contacts y ont automatiquement un `phone_number`.

### 4.3 Déclarer le webhook vers l'application

*Paramètres → Intégrations → Webhooks → Ajouter un webhook* :

| Champ | Valeur |
|---|---|
| URL | `http://host.docker.internal:8080/webhooks/chatwoot` |
| Événements | `message_created` (ajouter `conversation_updated` si besoin) |
| Secret | la valeur de `CHATWOOT_WEBHOOK_SECRET` |

- `host.docker.internal` est résolu par Docker Desktop vers le PC. Si besoin (Linux), utiliser
  `http://192.168.103.11:8080/webhooks/chatwoot`.
- Le compose autorise explicitement les appels vers le réseau local
  (`SAFE_FETCH_ALLOW_PRIVATE_NETWORK=true`), sinon la protection SSRF de Chatwoot refuse
  l'URL. **À ne pas conserver hors développement.**
- Pour modifier plus tard : le secret reste visible dans le formulaire d'édition du webhook.


## 5. Démarrer toute la pile, application accessible depuis le téléphone

`bash scripts/start.sh` démarre **tout** : les deux PostgreSQL, Redis, Chatwoot web, le worker
Sidekiq et l'application. Par défaut l'application écoute sur `127.0.0.1` (donc invisible depuis le
téléphone) ; surcharger `SERVER_ADDRESS` :

```bash
SERVER_ADDRESS=0.0.0.0 bash scripts/start.sh
```

Le script charge `.env.local`, attend la santé des conteneurs, lance le jar et attend
`/actuator/health`. Vérifier ensuite l'écoute :

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object LocalAddress, LocalPort
# LocalAddress doit être 0.0.0.0 (et non 127.0.0.1)
```

### 5.1 Ouvrir le pare-feu Windows (une seule fois, PowerShell **administrateur**)

```powershell
New-NetFirewallRule -DisplayName "mfish 8080" -Direction Inbound -Protocol TCP `
  -LocalPort 8080 -Action Allow -Profile Private
New-NetFirewallRule -DisplayName "Chatwoot 3000" -Direction Inbound -Protocol TCP `
  -LocalPort 3000 -Action Allow -Profile Private
```

### 5.2 Depuis le téléphone (même Wi-Fi)

- Interface d'administration : `http://192.168.103.11:8080/` → `admin` / `admin123`
  (profil `dev`).
- Chatwoot (vue agent) : `http://192.168.103.11:3000/`.
- Si l'IP Wi-Fi change :
  `Get-NetIPAddress -AddressFamily IPv4 | Where-Object InterfaceAlias -like '*Wi-Fi*'`
  puis mettre à jour `CHATWOOT_FRONTEND_URL` et redémarrer Chatwoot.

## 6. Tester la chaîne

### 6.1 Vérifier la réception du webhook sans Chatwoot

```bash
# Envoie un message "entrant" signé (même format que Chatwoot) à l'application.
# Le secret est repris de .env.local (CHATWOOT_WEBHOOK_SECRET).
bash scripts/webhook-chatwoot.sh
# → POST http://127.0.0.1:8080/webhooks/chatwoot -> 200
```

Puis suivre `logs/app.log` : la conversation est créée, l'IA est appelée, la réponse est envoyée
à Chatwoot (échec attendu si Chatwoot n'est pas démarré ou si `CHATWOOT_API_TOKEN` est vide).

### 6.2 Chaîne complète sans compte Meta (boîte API)

Simule un message client dans Chatwoot, traité par l'application puis renvoyé dans la
conversation. Remplacer `INBOX_IDENTIFIER` par la valeur de la section 4.2.

```bash
INBOX_IDENTIFIER="<inbox_identifier>"
BASE=http://localhost:3000/public/api/v1/inboxes/$INBOX_IDENTIFIER

# 1. Contact avec numéro de téléphone (sans phone_number, l'IA ne répond pas)
CONTACT=$(curl -s -X POST "$BASE/contacts" -H 'Content-Type: application/json' -d '{
  "identifier": "demo-telephone", "name": "Client Téléphone",
  "phone_number": "+237650000001" }')
CONTACT_IDENTIFIER=$(printf '%s' "$CONTACT" \
  | python -c "import sys,json;print(json.load(sys.stdin)['source_id'])")
echo "contact=$CONTACT_IDENTIFIER"

# 2. Conversation
CONVERSATION_ID=$(curl -s -X POST \
  "$BASE/contacts/$CONTACT_IDENTIFIER/conversations" \
  -H 'Content-Type: application/json' -d '{}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['id'])")
echo "conversation=$CONVERSATION_ID"

# 3. Message entrant (déclenche le webhook vers l'application)
curl -s -X POST \
  "$BASE/contacts/$CONTACT_IDENTIFIER/conversations/$CONVERSATION_ID/messages" \
  -H 'Content-Type: application/json' \
  -d '{"content":"Bonjour, je voudrais commander 2 poulets"}'
```

Attendu :

1. `docker compose --env-file .env.local logs -f chatwoot-worker` montre le job de webhook.
2. `logs/app.log` contient `Envoi d'un message Chatwoot (conversation …)`.
3. La conversation dans Chatwoot contient la réponse de l'IA (`outgoing`), visible aussi sur le
   téléphone connecté à `http://192.168.103.11:3000`.

### 6.3 Test « depuis le téléphone » avec le widget web

1. Créer une boîte **Website** dans Chatwoot et récupérer son `website_token`.
2. Renseigner `WEBSITE_TOKEN` dans `scripts/demo-chatwoot.html` (et `BASE_URL` si besoin).
3. Pré-créer le contact avec le même `identifier` **et un numéro de téléphone** (section 6.2,
   étape 1, avec l'`inbox_identifier` de la boîte Website).
4. Servir la page depuis le PC : `cd scripts && python -m http.server 8001`
5. Sur le téléphone : `http://192.168.103.11:8001/demo-chatwoot.html`, ouvrir la bulle et envoyer
   un message. La réponse de l'IA s'affiche dans le widget.

> L'application ignore les messages dont le contact n'a pas de `phone_number` : d'où l'étape 3.

### 6.4 Avec un vrai numéro WhatsApp (option)

Nécessite les identifiants Meta (`WHATSAPP_*`, `WHATSAPP_ENABLED=true`) **et** une URL publique
HTTPS pour le webhook Meta (tunnel ngrok/cloudflared) : voir `docs/WHATSAPP_SETUP.md`. Le
téléphone n'a alors pas besoin d'être sur le même Wi-Fi : WhatsApp passe par Internet.

## 7. Vérifications utiles

```bash
curl -s http://127.0.0.1:8080/actuator/health
docker compose --env-file .env.local logs --tail=100 chatwoot chatwoot-worker
tail -f logs/app.log        # événements webhook / conversation / IA
```

| Contrôle | Où |
|---|---|
| Webhook reçu et signé (200) | `ChatwootWebhookController` — `logs/app.log` |
| Idempotence (rejeu ignoré) | table `evenements_traites` |
| Mode de conversation IA/HUMAIN | table `conversations`, écran `/conversations` |
| Appel IA | `OrchestrateurIa` (DeepSeek) — `logs/app.log` |

## 8. Dépannage

| Symptôme | Cause probable | Action |
|---|---|---|
| `401` sur `/webhooks/chatwoot` | Secret différent entre Chatwoot et `CHATWOOT_WEBHOOK_SECRET`, ou horodatage > 5 min | Recopier le secret du webhook ; vérifier l'heure système |
| Webhook jamais reçu, aucun log côté app | Service `chatwoot-worker` arrêté | `docker compose --env-file .env.local up -d chatwoot-worker` |
| Webhook en échec dans Chatwoot (`UnsafeUrlError`) | Protection SSRF : URL privée refusée | `SAFE_FETCH_ALLOW_PRIVATE_NETWORK=true` (déjà dans le compose) |
| `Connection refused` côté Chatwoot | Application liée à `127.0.0.1` | Démarrer avec `SERVER_ADDRESS=0.0.0.0` |
| Contact créé mais aucune réponse IA | Message `outgoing`, note privée, conversation en `HUMAIN`, ou `phone_number` absent | Vérifier le mode dans `/conversations`, ajouter le numéro au contact |
| Réponse dans les logs mais pas dans Chatwoot | `CHATWOOT_API_TOKEN` vide/invalide, `CHATWOOT_ACCOUNT_ID` erroné | Régénérer le jeton profil, redémarrer l'application |
| Widget muet / page blanche sur le téléphone | `CHATWOOT_FRONTEND_URL` en `localhost` ou pare-feu fermé | Mettre l'IP LAN, autoriser le port 3000 |
| Chatwoot boucle au démarrage | Base non préparée | `docker compose --env-file .env.local run --rm chatwoot bundle exec rails db:chatwoot_prepare` |
| `PG::FeatureNotSupported: extension "vector" is not available` pendant `db:chatwoot_prepare` | `chatwoot-postgres` basé sur `postgres:16-alpine` (sans pgvector) | Le compose impose `pgvector/pgvector:pg16` : recréer le service (`docker compose --env-file .env.local up -d --force-recreate chatwoot-postgres`) ; si l'ancien volume a été créé avec `postgres:16-alpine`, le supprimer (`docker volume rm mfish_chatwoot_postgres_data`) puis relancer `bash scripts/start.sh` |
| Conteneur `chatwoot` arrêté aussitôt (`Exited (0)`), log `Switch to inspect mode.` | L'image `chatwoot/chatwoot` a pour commande par défaut `irb` | Le compose fournit `command: bundle exec rails s -b 0.0.0.0 -p 3000` : recréer le service (`docker compose --env-file .env.local up -d --force-recreate chatwoot`) |
| `Migration checksum mismatch for migration version N` au démarrage de l'app | Jar compilé avant la dernière migration Flyway | Relancer `bash scripts/start.sh` : il détecte un jar périmé (`target/.build-ok`) et recompile |

## 9. Limites connues

- Le profil `prod` (conteneur `app`) force `server.servlet.session.cookie.secure=true` :
  l'interface n'est utilisable qu'en **HTTPS**. Pour un test LAN en HTTP, lancer le jar avec le
  profil `dev` (section 5).
- Le conteneur `app` exige en plus `APP_JWT_SECRET` (≥ 32 octets) et `APP_ADMIN_PASSWORD_HASH`.
- `chatwoot/chatwoot:latest` évolue : relancer `db:chatwoot_prepare` après chaque mise à jour.
- `SAFE_FETCH_ALLOW_PRIVATE_NETWORK=true` est un réglage de **développement** uniquement.

