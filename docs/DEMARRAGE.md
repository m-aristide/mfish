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
| Géocodage | `GEOCODING_PROVIDER` |
| Compose (PostgreSQL) | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` (défaut `5433`) |

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
bash scripts/start.sh       # charge .env.local, démarre la base, démarre l'app, attend UP
bash scripts/stop.sh        # arrête l'app (via PID) puis la base
bash scripts/status.sh      # état base + app + derniers logs
bash scripts/smoke-test.sh  # rejoue les vérifications (sans appels externes)

# Sous Linux / macOS / WSL :
./scripts/start.sh
```

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
- Logs applicatifs : `logs/app.log`

## Ports

- Application : **8080** (localhost uniquement, `SERVER_ADDRESS=127.0.0.1`)
- PostgreSQL (mfish) : **5433** (le port **5432** est déjà occupé par un autre PostgreSQL sur cette
  machine — le service du projet utilise `${POSTGRES_PORT:-5433}`)

## Intégrations externes — état vérifié

| Service | État | Détail |
|---|---|---|
| PostgreSQL | ✅ réel | `docker-compose.yml`, Flyway (6 migrations), port 5433 |
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
5. **Port 5432 occupé** : un autre PostgreSQL tourne déjà. Le service du projet utilise le port 5433.

## Identifiants de connexion (interface web)

- **Utilisateur** : `admin`
- **Mot de passe** : défini par `app.admin.password-hash` — soit `APP_ADMIN_PASSWORD_HASH`
  (`.env.local`), soit le défaut dev `{noop}admin123` (`application-dev.yaml`).

## Notes de sécurité

- Aucun secret versionné : `.env.local` est ignoré ; `application.yaml` n'utilise que des références
  `${VARIABLE}`.
- ⚠️ L'historique Git contient d'anciennes valeurs en dur (DeepSeek/RouteXL) dans `application.yaml` ;
  elles ont été retirées de la copie de travail. **Faire tourner (révoquer) ces clés.**
