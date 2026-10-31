# mfish — Gestion de livraison pour commerçant

Application **Spring Boot 4.1.2 / Java 25** de gestion de livraison : prise de commande
WhatsApp automatisée par IA (DeepSeek), enregistrement des commandes (PostgreSQL), génération
d'une tournée optimisée (RouteXL), notification des clients (Chatwoot) et interface PWA.

> Projet en construction. Voir `docs/DECISIONS.md` pour les choix techniques.

## Prérequis

- **JDK 25** (ex. Eclipse Temurin). Sur cette machine, `JAVA_HOME` pointe sur le JDK 17 :
  forcer le JDK 25 pour construire (voir ci-dessous).
- **Docker** (pour PostgreSQL et Testcontainers).
- Maven fourni via le wrapper (`mvnw`/`mvnw.cmd`) — pas d'installation requise.

## Configuration

Toutes les valeurs sensibles sont injectées par **variables d'environnement** (aucune valeur
réelle n'est commitée). Copier `.env.example` vers `.env` et renseigner les valeurs :

```powershell
Copy-Item .env.example .env
```

Variables principales : `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `APP_JWT_SECRET`, `APP_ADMIN_PASSWORD_HASH`,
`CHATWOOT_*`, `DEEPSEEK_*`, `ROUTEXL_*`, `APP_TIMEZONE`.

Profil par défaut : `dev` (base locale `jdbc:postgresql://localhost:5432/mfish`).

## Lancer l'application

### Local (profil dev)

```powershell
# 1. Base de données
docker run --name mfish-postgres -e POSTGRES_DB=mfish -e POSTGRES_USER=mfish `
  -e POSTGRES_PASSWORD=mfish -p 5432:5432 -d pgvector/pgvector:pg16

# 2. Application (JDK 25 obligatoire)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.5.0.3"
.\mvnw.cmd spring-boot:run
```

### Docker (tout le stack)

```powershell
docker compose up --build
```

## Tests et couverture

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.5.0.3"

# Lancer tous les tests
.\mvnw.cmd test

# Build complet + vérification des seuils JaCoCo (90 % lignes / 85 % branches)
.\mvnw.cmd verify

# Rapport de couverture (après verify)
start target/site/jacoco/index.html
```

Les tests d'intégration utilisent **Testcontainers** (PostgreSQL) : Docker doit être démarré.
Aucun appel réseau réel : Chatwoot, DeepSeek et RouteXL sont simulés (mocks ou WireMock).

## Migrations

Gérées uniquement par **Flyway** (`src/main/resources/db/migration`),
`spring.jpa.hibernate.ddl-auto=validate`.

## Déploiement

- `Dockerfile` multi-étapes (JDK 25 → JRE 25, utilisateur non root).
- `docker-compose.yml` : application + PostgreSQL + Chatwoot (+ Redis + sa base).
- CI GitHub Actions : `.github/workflows/ci.yml` (build + tests + couverture).
