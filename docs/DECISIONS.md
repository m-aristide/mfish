# Décisions techniques

Ce document consigne les choix effectués en cas d'ambiguïté. Il est complété au fil du projet.

## Contexte

- **Java 25**, **Spring Boot 4.1.2** (Spring Framework 7, Jakarta EE 11, Jackson 3, Hibernate 7).
- Package racine : `com.mfish.erp`. Organisation **par domaine** (pas par couche technique globale).
- Build : Maven (wrapper). Base : PostgreSQL 16 via Testcontainers pour les tests.

## Décisions (étape 1 — fondations)

### 1. Sécurité : JWT stateless (HMAC-SHA256)
- **Choix** : authentification par jeton JWT signé en **HS256**, filtres Spring Security.
- **Raison** : la PWA consomme l'API depuis une autre origine ; un jeton stateless évite la
  session serveur et simplifie la mise à l'échelle. Le préfixe `app.jwt.*` était déjà présent
  dans `application.yaml`.
- **CSRF** : désactivé sur les routes `/api/**` (API sans cookie de session) ; le reste est
  configuré de façon cohérente dans le module `securite`.
- **JWT** : bibliothèque **Nimbus JOSE JWT 10.9.1**, apportée par `spring-security-oauth2-jose`
  (version gérée par la BOM Spring Security 7.1.2). L'API Nimbus 10 a été vérifiée dans le jar
  résolu (`MACSigner`, `MACVerifier`, `SignedJWT`, `JWTClaimsSet`).

### 2. Résilience : Spring Retry
- **Choix** : `spring-retry` 2.0.13 (hors BOM Spring Boot, version épinglée et vérifiée sur
  Maven Central).
- **Raison** : plus léger que Resilience4j ; le retry avec backoff est piloté par
  `RetryProperties` (record validé), sans AOP (`RetryTemplate` programmatique).

### 3. Géocodage : Nominatim
- **Choix** : fournisseur `nominatim` (OpenStreetMap) par défaut, déjà configuré via
  `app.geocoding.*`. Adresse/quartier → coordonnées GPS, repli sur les coordonnées du quartier.

### 4. Montants monétaires en entiers
- **Choix** : tous les montants sont des `long` exprimés dans la plus petite unité monétaire
  (jamais de `double`/`float`). `CHECK` SQL sur montants >= 0.

### 5. Configuration typée et validée au démarrage
- **Choix** : records `@ConfigurationProperties` immuables et `@Validated`. Les secrets ne sont
  **jamais** commités : valeurs injectées par variables d'environnement. En développement, des
  valeurs de convenance (`{noop}admin123`, secret JWT « dev-only ») sont fournies dans
  `application-dev.yaml` ; en production, l'absence de valeur fait échouer le démarrage
  (fail-fast).

### 6. Schéma géré uniquement par Flyway
- **Choix** : `spring.jpa.hibernate.ddl-auto=validate` ; migrations `V1__...`, `V2__...`, etc.
  dans `src/main/resources/db/migration`.

### 7. JDK 25 et build local
- **Constat** : `JAVA_HOME` système pointe sur le JDK 17 alors que le projet exige Java 25.
  Pour construire : `JAVA_HOME="C:\Program Files\Java\jdk-25.0.5.0.3" ./mvnw ...` (Windows) ou
  équivalent. Documenté dans le README.

### 8. Horloge injectable
- **Choix** : `java.time.Clock` bean, calé sur `app.timezone` ; le code métier n'appelle jamais
  `Instant.now()` directement.

### 9. Seuils de couverture
- **Choix** : JaCoCo avec seuils **90 % lignes / 85 % branches** appliqués à `mvn verify`
  (déjà configurés dans `pom.xml`).

### 10. Lombok retiré
- **Choix** : suppression de la dépendance Lombok (optionnelle) et des `annotationProcessorPaths`
  du `maven-compiler-plugin`.
- **Raison** : Lombok n'était utilisé dans aucun fichier source et produisait un warning de
  compilation (`sun.misc.Unsafe`). Le projet utilise exclusivement les `record` Java.

### 11. Image PostgreSQL : pgvector/pgvector:pg16
- **Choix** : utilisation de `pgvector/pgvector:pg16` (PostgreSQL 16 + extension `pgvector`)
  pour la base de test et le `docker-compose`.
- **Raison** : l'image `postgres:16-alpine` était bloquée au téléchargement dans cet
  environnement ; `pgvector/pgvector:pg16` est déjà disponible localement, satisfait la
  contrainte « PostgreSQL 15+ » et pourra servir pour un éventuel rapprochement de produits par
  similarité (embeddings), sans être requis par le noyau métier.

### 12. Packages des annotations de test réorganisés (Spring Boot 4)
- **Constat** : les annotations de *slices* de test ont changé de package en Spring Boot 4.1.2.
  - `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
  - `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
  - `@SpringBootTest` → inchangé (`org.springframework.boot.test.context.SpringBootTest`)
- **Raison** : vérifié dans les jars résolus (`spring-boot-data-jpa-test`, `spring-boot-webmvc-test`),
  suite à l'erreur de compilation du package historique `...boot.test.autoconfigure.orm.jpa`.

### 13. Audit des entités (created_at / updated_at)
- **Choix** : les métadonnées d'audit (`created_at`/`updated_at`, `evenements_log.created_at`)
  sont alimentées par Hibernate (`@CreationTimestamp`/`@UpdateTimestamp`).
- **Raison** : l'audit Spring Data (`@EnableJpaAuditing`) n'était pas appliqué dans le contexte
  `@DataJpaTest` de Spring Boot 4 (violations NOT NULL). La logique **métier** continue d'utiliser
  exclusivement le `Clock` injecté ; seuls les horodatages d'audit relèvent d'Hibernate.

### 14. Module tournée : contrat RouteXL simplifié
- **Choix** : l'adaptateur `RouteXlClient` envoie `POST /tour` avec un contrat JSON simplifié
  (`start` + `visites`) et lit une réponse `route.visits[{id, arrival (HH:mm)}]`, testé par WireMock.
- **Raison** : le format exact de l'API RouteXL sera validé/ajusté contre l'API réelle ; le plan B
  local (`OptimisationLocale` : tri `ordre_defaut` + plus proche voisin, Haversine) garantit le
  fonctionnement même sans service externe. Retry via Spring Retry, timeouts configurés.

### 15. Module IA : Jackson 3 et contrat JSON
- **Choix** : le parsing de la sortie IA utilise **Jackson 3** (`tools.jackson.databind.json.JsonMapper`,
  `tools.jackson.databind.DeserializationFeature`), avec `FAIL_ON_UNKNOWN_PROPERTIES` désactivé
  (champs en trop ignorés). Le contrat JSON utilise des noms de champs **camelCase** (`dateSouhaitee`)
  pour éviter l'annotation `@JsonProperty` (jackson-annotations 3 non résolu).
- **Raison** : Spring Boot 4 fournit Jackson 3 (package `tools.jackson`, pas `com.fasterxml`).
  L'intention est stockée en `String` puis convertie via `Intention.fromString` (tolérant à la casse).
  Le prix est **toujours lu en base** (l'IA ne fournit jamais de prix).

### 16. Webhook Chatwoot : signature et asynchronisme
- **Choix** : vérification HMAC-SHA256 via l'en-tête `X-Chatwoot-Signature` (secret partagé
  `CHATWOOT_WEBHOOK_SECRET`), idempotence via `evenements_traites`, filtrage des messages sortants
  et notes privées, bascule IA→HUMAIN sur réponse d'un agent. Traitement asynchrone via le
  `TaskExecutor` (threads virtuels Java 25).
- **Raison** : répondre 200 rapidement à Chatwoot puis traiter hors-ligne. Le nom exact de
  l'en-tête Chatwoot sera validé dans `docs/CHATWOOT_SETUP.md`.
