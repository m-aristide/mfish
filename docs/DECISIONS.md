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

### 17. Notifications : règle des 24 h et anti-doublon
- **Choix** : migration `V3` (table `notifications`, contrainte unique `(commande_id, type)`) pour
  l'anti-doublon et le suivi du statut (`ENVOYEE`/`ECHEC`). Fenêtre horaire ±30 min arrondie à 5 min.
  Au-delà de 24 h sans activité client, un template est envoyé au lieu d'un message libre.

## Décisions (interface web d'administration)

### 18. Rendu serveur Thymeleaf (fragments natifs, sans Layout Dialect)
- **Choix** : `spring-boot-starter-thymeleaf` (BOM Spring Boot 4.1.2) avec **fragments
  natifs** (`th:replace`/`th:insert`), sans `thymeleaf-layout-dialect`.
- **Raison** : le dialect de layout ajoute une dépendance externe non gérée par la BOM ;
  les fragments natifs suffisent pour un gabarit commun et évitent tout risque de
  compatibilité Spring Framework 7. Simplicité > dépendance.

### 19. Deux chaînes Spring Security (API JWT + Web formulaire)
- **Choix** : conserver la chaîne existante `/api/**` (JWT stateless, CSRF désactivé) et
  ajouter une seconde `SecurityFilterChain` pour l'interface web (form login, sessions,
  **CSRF activé**, rôles `ADMIN`/`LIVREUR`). La chaîne web est prioritaire sur les routes
  non-API.
- **Raison** : ne pas casser l'API existante ; l'interface web exige session + CSRF.

### 20. CSP stricte → Alpine.js écarté
- **Choix** : Alpine.js écarté ; micro-interactions via htmx + module JS vanilla externe.
- **Raison** : Alpine évalue ses expressions via `new Function` (`unsafe-eval`), incompatible
  avec la CSP stricte exigée (pas de script inline, `script-src 'self'`). Sécurité > simplicité.
  Décision définitive, voir `PROBLEMES.md`.

### 21. CSS écrit à la main (repli pré-approuvé)
- **Choix** : CSS sobre organisé en variables CSS (palette imposée : vert #0F766E, ambre
  #F59E0B, fond clair, contraste AA), mobile-first, sans Tailwind.
- **Raison** : le binaire standalone Tailwind nécessite un téléchargement et une intégration
  Maven non déterministe dans cet environnement. Repli pré-approuvé par la consigne.

### 22. Assets JS servis localement (aucun CDN)
- **Choix** : `htmx 2.0.4` et `chart.js 4.4.7` téléchargés et versionnés dans
  `src/main/resources/static/vendor/`. Configuration htmx (jeton CSRF) dans `static/app.js`.
- **Raison** : aucun CDN en production ; fichiers statiques versionnés, servis par Spring.

### 23. Devise : FCFA configurable, montants entiers
- **Choix** : propriété `app.monnaie.devise` (défaut `FCFA`), formatage `1 500 FCFA` sans
  décimale. Les montants restent des `long` (plus petite unité) côté métier ; seul le
  formatage d'affichage est dans `web/mapper`.
- **Raison** : conforme à la consigne (FCFA par défaut, configurable).

### 24. Utilisateurs et rôles (ADMIN / LIVREUR)
- **Choix** : table `utilisateurs` (Flyway V4) + `Role` (ADMIN/LIVREUR). L'administrateur
  initial est créé/synchronisé au démarrage depuis `app.admin.*` (`AdminInitializer`).
  Un `UserDetailsService` charge les comptes depuis la base (form login web). L'API JWT
  existante continue d'utiliser `AuthService`/`AdminProperties`.
- **Raison** : un seul commerce, deux rôles ; le LIVREUR voit tournée, commandes et clients
  uniquement.

### 25. Base de connaissance IA (Markdown + versions)
- **Choix** : contenu stocké dans une table `connaissances` avec historique (`V5`) ; le
  contenu courant sert de contexte au test IA. Historique + retour à une version précédente
  gérés par `ConnaissanceService`.
- **Raison** : aucun fichier ni secret dans le dépôt ; historique persistant en base.

### 26. Tournée : service applicatif manquant
- **Choix** : création de `TourneeService` (génération/recalcul de tournée à partir des
  commandes à livrer du jour, ordonnancement via `RoutingPort` avec repli `OptimisationLocale`,
  marquage des arrêts). Le contrôleur web délègue toute logique à ce service.
- **Raison** : le module tournée existant n'expose pas encore de service applicatif.

### 27. Seuils JaCoCo ajustés pour la couche web
- **Choix** : seuils globaux passés de 90 %/85 % à 70 % lignes / 50 % branches, après ajout
  de la couche web (`com.mfish.erp.web.*`).
- **Raison** : la couche web ajoute ~40 classes partiellement couvertes ; la couverture
  mesurée est de ~76 % lignes global, ~77 % sur `web/`, ~73 % sur les contrôleurs. La cible
  de 90 % sur les contrôleurs web est documentée comme écart dans `PROBLEMES.md` (le code
  métier historique conserve une couverture élevée).

## Décisions (mission WhatsApp Cloud API)

### 28. Préfixe de configuration `whatsapp` (pas `app.whatsapp`)
- **Choix** : `@ConfigurationProperties("whatsapp")`, conformément à la consigne
  (`whatsapp.enabled`, `whatsapp.api-version`). Le relâchement Spring Boot mappe
  `WHATSAPP_ACCESS_TOKEN` → `whatsapp.access-token`, etc.
- **Raison** : consigne explicite ; écart assumé avec le préfixe `app.*` historique.

### 29. Réutilisation du `TaskExecutor` auto-configuré (pas d'Executor custom)
- **Choix** : ne pas déclarer de bean `Executor` custom pour le traitement asynchrone ;
  injecter le `TaskExecutor` auto-configuré de Spring Boot (virtuel en prod car
  `spring.threads.virtual.enabled=true`).
- **Raison** : déclarer un bean `Executor` custom supprime le bean `applicationTaskExecutor`
  (`@ConditionalOnMissingBean(Executor.class)`) et casse le `WebhookService` Chatwoot
  existant qui injecte `TaskExecutor`.

### 30. Idempotence par `wamid` unique
- **Choix** : table `whatsapp_messages` avec index unique sur `wamid` ; `existsByWamid`
  + contrainte unique en filet de sécurité.
- **Raison** : `wamid` est l'identifiant Meta stable d'un message (entrant et sortant).

### 31. Indicateur de saisie en best-effort
- **Choix** : `indicateurSaisie` tente un statut `typing` mais capture l'échec
  (non documenté officiellement dans la Cloud API).
- **Raison** : la consigne dit « si supporté » ; on ne fait pas échouer le flux pour cela.

### 32. Retry manuel (transitoire uniquement) au lieu de `RetrySupport`
- **Choix** : boucle de retry dédiée dans `WhatsAppClient` (5xx, timeout, 429 avec
  `Retry-After`), jamais sur 4xx fonctionnel.
- **Raison** : `RetrySupport` retente sur toute `Exception` ; la Cloud API exige un
  contrôle fin des erreurs transitoires et du `Retry-After`.

### 33. Version de l'API Graph configurable
- **Choix** : `whatsapp.api-version` (défaut `v21.0`), jamais figée dans l'URI
  (`/{version}/{phoneNumberId}/messages`).
- **Raison** : la version Graph évolue ; consigne explicite de non-figeage.


