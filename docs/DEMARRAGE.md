# DÉMARRAGE — mfish

Guide pour démarrer, arrêter et vérifier l'application **Spring Boot 4.1.2 / Java 25** en local.

## Prérequis

- **JDK 25** (chemin constaté sur cette machine : `C:\Program Files\Java\jdk-25.0.5.0.3`).
  `JAVA_HOME` système pointe sur le JDK 17 : forcer le JDK 25 pour construire.
- **Docker** + **Docker Compose** (PostgreSQL via `pgvector/pgvector:pg16`).
- **Git Bash** (pour exécuter les scripts `.sh` sous Windows).
- Maven fourni via le wrapper (`mvnw.cmd` / `mvnw`) — aucune installation requise.

## Configuration

Créer `.env.local` à la racine à partir de `.env.example`, puis renseigner les valeurs réelles.
**`.env.local` est dans `.gitignore` : aucun secret n'est versionné.**

Noms des variables (uniquement les noms, jamais les valeurs ici) :

| Domaine | Variables |
|---|---|
| Base de données | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Sécurité | `APP_JWT_SECRET` (≥ 32 octets), `APP_ADMIN_PASSWORD_HASH` (`{bcrypt}...` ou `{noop}...`) |
| CORS / divers | `APP_CORS_ALLOWED_ORIGINS`, `APP_TIMEZONE` |
| Chatwoot (WhatsApp) | `CHATWOOT_BASE_URL`, `CHATWOOT_API_TOKEN`, `CHATWOOT_ACCOUNT_ID`, `CHATWOOT_WEBHOOK_SECRET` |
| DeepSeek (IA) | `DEEPSEEK_API_KEY`, `DEEPSEEK_MODEL` |
| RouteXL (tournée) | `ROUTEXL_USERNAME`, `ROUTEXL_PASSWORD`, `ROUTEXL_MAX_STOPS` |
| Géocodage (Google) | `GOOGLE_GEOCODING_API` |
| Compose (PostgreSQL) | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |

### Valeurs de développement (profil `dev`)

Si une variable est absente ou vide, le profil `dev` (`application-dev.yaml`) fournit des valeurs de
convenance **non sensibles** :

- `app.admin.password-hash = {noop}admin123` → connexion `admin` / `admin123`
- `app.jwt.secret = dev-only-secret-...`
- `app.chatwoot.webhook-secret = dev-webhook-secret`

## Démarrage / arrêt

Scripts dans `scripts/` (sans aucun secret en dur, idempotents) :

```bash
# Sous Windows (Git Bash) :
bash scripts/start.sh       # démarre TOUT : bases, Redis, Chatwoot web + worker, application
bash scripts/stop.sh        # arrête TOUT (application, worker, conteneurs — données conservées)
bash scripts/status.sh      # état des conteneurs + application + Chatwoot + derniers logs
bash scripts/smoke-test.sh  # rejoue les vérifications (sans appels externes)

# Sous Linux / macOS / WSL :
./scripts/start.sh
```

`start.sh` est idempotent (relançable à tout moment) et :

1. charge `.env.local` (aucune valeur vide, aucun secret en dur) ;
2. démarre `postgres`, `chatwoot-postgres`, `redis` et attend leur santé ;
3. initialise le schéma Chatwoot (`db:chatwoot_prepare`) **seulement** au premier lancement,
   puis démarre `chatwoot` et `chatwoot-worker` (le worker Sidekiq est indispensable aux webhooks) ;
4. recompile le jar s'il est absent ou périmé (sources plus récentes), démarre l'application
   et attend `/actuator/health`.

Options utiles :

| Commande | Effet |
|---|---|
| `bash scripts/start.sh` | tout, application sur `127.0.0.1` |
| `SERVER_ADDRESS=0.0.0.0 bash scripts/start.sh` | tout, application accessible au réseau local (téléphone) |
| `SKIP_CHATWOOT=1 bash scripts/start.sh` | sans Chatwoot |
| `SKIP_APP=1 bash scripts/start.sh` | infrastructure seule (bases, cache, Chatwoot) |
| `bash scripts/stop.sh` | arrêt complet, conteneurs conservés |
| `bash scripts/stop.sh --down` | arrêt + suppression des conteneurs (données conservées) |
| `bash scripts/stop.sh --volumes` | arrêt + suppression des conteneurs **et des données** |

`smoke-test.sh` accepte l'option `--with-external` pour déclencher **un** appel réel DeepSeek et
**un** appel réel RouteXL (consommation de quota — à n'utiliser qu'à la demande).

### Sans les scripts

```powershell
# Base
docker compose up -d postgres

# Application (JDK 25 obligatoire)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.5.0.3"
$env:SERVER_ADDRESS = "127.0.0.1"
java -Djavax.net.ssl.trustStoreType=Windows-ROOT -jar target\erp-0.0.1-SNAPSHOT.jar
```

## Adresses

- Interface web : http://127.0.0.1:8080/ (redirige vers `/connexion`)
- Health check : http://127.0.0.1:8080/actuator/health
- Chatwoot (si démarré) : http://127.0.0.1:3000/ — voir `docs/CHATWOOT_SETUP.md`
- Logs applicatifs : `logs/app.log`

## Chatwoot et test depuis un téléphone (même Wi-Fi)

Procédure complète (initialisation, webhook, boîte API, pare-feu, tests) :
**`docs/CHATWOOT_SETUP.md`**.

```bash
# Toute la pile (bases, Redis, Chatwoot web + worker, application) joignable depuis le téléphone
SERVER_ADDRESS=0.0.0.0 bash scripts/start.sh

# Injection d'un webhook Chatwoot signé, sans passer par Chatwoot
bash scripts/webhook-chatwoot.sh
```

## Ports

- Application : **8080** (par défaut `SERVER_ADDRESS=127.0.0.1`, donc local uniquement ;
  `SERVER_ADDRESS=0.0.0.0` pour l'exposer au réseau local / téléphone)
- Chatwoot : **3000** (services Docker `chatwoot` + `chatwoot-worker`)
- PostgreSQL (mfish) : **5432** (`docker-compose.yml` publie `"5432:5432"`, volume `postgres_data`)

## Intégrations externes — état vérifié

| Service | État | Détail |
|---|---|---|
| PostgreSQL | ✅ réel | `docker-compose.yml`, Flyway (6 migrations), port 5432 |
| DeepSeek | ✅ réel | appel de test réussi (réponse cohérente) ; TLS via magasin Windows |
| RouteXL | ✅ réel | calcul de tournée 3 arrêts réussi (ordre optimisé + heures) |
| WhatsApp/Chatwoot | 🔶 simulé | aucun envoi réel ; webhook simulé : signature valide → 200 + enregistrement, signature invalide → 401 |

## Dépannage

1. **`Failed to delete ...\target`** (build) : fichier verrouillé par l'IDE. Supprimer `target`
   manuellement puis relancer `clean verify`.
2. **`APP_JWT_SECRET doit faire au moins 32 octets`** : la valeur de `.env.local` est trop courte.
   La commenter (le profil `dev` fournit un secret valide) ou mettre une valeur ≥ 32 octets.
3. **`password authentication failed for user "mfish"`** : fin de ligne CRLF dans `.env.local` chargé
   par un script bash. Le loader de `start.sh` retire désormais les `\r`.
4. **`certificate_unknown / PKIX path building failed`** (DeepSeek/RouteXL) : le JVM ne valide pas le
   certificat. Lancer avec `-Djavax.net.ssl.trustStoreType=Windows-ROOT` (déjà appliqué par `start.sh`).
5. **Port 5432 déjà occupé** : un autre PostgreSQL tourne sur la machine. Arrêter ce service, ou
   adapter le mapping `ports:` du service `postgres` **et** `SPRING_DATASOURCE_URL` dans `.env.local`
   (les deux doivent utiliser le même port).
6. **`Migration checksum mismatch for migration version N`** : le jar a été compilé avant la dernière
   migration Flyway. `bash scripts/start.sh` détecte un jar périmé (`target/.build-ok`), arrête
   l'application si nécessaire puis recompile.
7. **`Unable to rename ...jar to ...jar.original`** pendant la compilation : le jar est verrouillé
   par une instance en cours. `start.sh` arrête désormais l'application avant de recompiler ;
   sinon `bash scripts/stop.sh` puis relancer.
8. **Connexion administrateur refusée après avoir renseigné `APP_ADMIN_PASSWORD_HASH`** (web →
   `/connexion?erreur`, API `/api/auth/login` → 401, **y compris avec l'ancien mot de passe**) :
   Docker Compose interpole les `$` lus dans `.env`. Un hash collé brut (`{bcrypt}$2b$12$XCxL...`)
   est tronqué à `{bcrypt}$2b$12` : la partie alphanumérique qui suit le dernier `$` est lue comme
   un nom de variable non définie, remplacée par du vide, et `AdminInitializer` recopie cette
   valeur tronquée dans `utilisateurs` au démarrage. **Doubler chaque `$`** :
   `APP_ADMIN_PASSWORD_HASH={bcrypt}$$2b$$12$$XCxL...`, puis **recréer** le conteneur
   (`docker compose up -d --no-deps app` ; un `restart` ne relit pas `.env`).
   Contrôle : `docker exec <conteneur> printenv APP_ADMIN_PASSWORD_HASH | wc -c` doit renvoyer 69
   (68 caractères + saut de ligne) et `docker compose config | grep APP_ADMIN` doit afficher la
   valeur complète.

## Identifiants de connexion (interface web)

- **Utilisateur** : `admin`
- **Mot de passe** : défini par `app.admin.password-hash` — soit `APP_ADMIN_PASSWORD_HASH`
  (`.env.local`), soit le défaut dev `{noop}admin123` (`application-dev.yaml`).

### Régénérer le hash (rotation du mot de passe)

```bash
# Génère un mot de passe aléatoire et son hash BCrypt au format Spring ({bcrypt}$2b$12$...)
python3 -c "import crypt,secrets,string; p=''.join(secrets.choice(string.ascii_letters+string.digits) for _ in range(28)); print(p); print('{bcrypt}'+crypt.crypt(p, crypt.mksalt(crypt.METHOD_BLOWFISH)))"
# Variante sans Python : htpasswd -bnBC 12 "" 'motdepasse' | tr -d ':\n'   (hash $2y$)
```

Dans un `.env` lu par Docker Compose, **doubler les `$`** de la valeur avant de la coller :

```bash
# {bcrypt}$2b$12$XCxL...  ->  {bcrypt}$$2b$$12$$XCxL...
APP_ADMIN_PASSWORD_HASH={bcrypt}$$2b$$12$$XCxL...
```

Puis recréer le conteneur (`docker compose up -d --no-deps app`) : `AdminInitializer` resynchronise
la table `utilisateurs` au démarrage. Un `docker compose restart` **ne relit pas** `.env`.

## Notes de sécurité

- Aucun secret versionné : `.env.local` est ignoré ; `application.yaml` n'utilise que des références
  `${VARIABLE}`.
- ⚠️ L'historique Git contient d'anciennes valeurs en dur (DeepSeek/RouteXL) dans `application.yaml` ;
  elles ont été retirées de la copie de travail. **Faire tourner (révoquer) ces clés.**
