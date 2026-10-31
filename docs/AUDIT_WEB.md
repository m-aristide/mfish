# Audit de l'interface web d'administration

Date : 2026-10-31. Objectif : construire l'interface web d'administration (Thymeleaf,
rendu serveur) sur le backend Spring Boot 4.1.2 / Java 25 existant.

## Ce qui existe déjà (réutilisé)

### Socle technique
- **Build** : Maven (wrapper `mvnw.cmd`), parent Spring Boot **4.1.2**, Java **25**.
  La BOM gère les versions des starters. JDK 25 requis : `JAVA_HOME` système pointe sur
  JDK 17, on force `C:\Program Files\Java\jdk-25.0.5.0.3`.
- **Starter web** : `spring-boot-starter-webmvc` (nom Spring Boot 4). Actuator,
  validation, JPA, Flyway, Security, RestClient déjà présents.
- **Tests** : JUnit 5, AssertJ, Mockito, MockMvc (`spring-boot-webmvc-test`),
  `@WebMvcTest` dans `org.springframework.boot.webmvc.test.autoconfigure` (Spring Boot 4),
  Testcontainers PostgreSQL (voir `PROBLEMES.md` : Docker indisponible → H2).

### Backend (domaine, à réutiliser tel quel)
- **Client** : `ClientService` (lister/trouver/creer/modifier/supprimer), `ClientRequest`,
  `ClientDto`, `ClientRepository` (recherche par téléphone).
- **Produit** : `ProduitService`, `ProduitRequest`, `ProduitDto`, `ProduitRepository`
  (`findByActifTrueOrderByNomAsc`).
- **Quartier** : `QuartierService`, `QuartierRequest`, `QuartierDto`.
- **Commande** : `CommandeService` (machine à états, encaissement, annulation),
  `CommandeApplicationService` (résolution références, prix en base), `CommandeRequest`,
  `CommandeDto`, `LigneCommandeDto`, `StatutCommande` (A_PLANIFIER/PLANIFIEE/EN_COURS/LIVREE/ANNULEE),
  `StatutPaiement`, `ModePaiement`, `SourceCommande`.
- **Tournée** : entités `Tournee`, `ArretTournee`, repositories, ports `RoutingPort` /
  `GeocodingPort`, plan B `OptimisationLocale`, `DistanceHaversine`, records
  `ArretPlanifie`, `ArretAOptimiser`, `Coordonnees`, `StatutArret`, `StatutTournee`.
  **Absent** : service applicatif + contrôleur → à créer (`TourneeService`).
- **Conversation** : entité `Conversation`, `ModeConversation` (IA/HUMAIN),
  `ConversationRepository`, port `MessagingPort` (envoyerMessage/etiqueterConversation).
  **Absent** : service applicatif + contrôleur → à créer (`ConversationService`).
- **Statistiques** : `StatsService`, `TableauDeBordService`, `TableauDeBordDto`,
  `EncaissementDto`, `CaParQuartier`, `TopProduit`, contrôleurs REST `/api/stats/**`,
  `/api/tableau-de-bord`.
- **Paramètres** : `ParametreService` (clé/valeur), `ParametreRepository`.
- **IA** : `OrchestrateurIa`, `LlmPort`, `PromptBuilder`, `ParseurReponseIa`,
  `RapprochementProduits`. **Absent** : base de connaissance + historique → à créer
  (`ConnaissanceService`).
- **Sécurité (API)** : `SecurityConfig` (JWT stateless, CSRF désactivé sur `/api/**`),
  `JwtAuthenticationFilter`, `AuthService`, `LoginRateLimiter`, `AdminProperties`
  (`app.admin.username` / `app.admin.password-hash`), `PasswordEncoder` (Delegating).

### Migrations Flyway
- `V1__init_schema.sql` (schéma complet), `V2__seed_demo.sql` (données démo),
  `V3__notifications.sql`.

### Configuration
- `application.yaml` (+ `application-dev.yaml`, `application-prod.yaml`), secrets par
  variables d'environnement, `app.timezone=Africa/Abidjan`, `app.admin.*`,
  `app.chatwoot.*` (base-url disponible pour le lien de réponse manuelle).

## Ce qui est créé (nouveau)

### Couche web (nouveau package `com.mfish.erp.web.*`)
- `web/controller/` : un contrôleur par écran (connexion, tableau de bord, commandes,
  tournée, clients, produits, conversations, base de connaissance, statistiques,
  paramètres). Aucune logique métier.
- `web/dto/` : records de formulaires et de vues (jamais d'entité JPA exposée).
- `web/mapper/` : mappers entité/DTO → vue.
- `web/advice/` : `@ControllerAdvice` (attributs globaux : utilisateur courant, rôle,
  devise ; gestion d'erreurs renvoyant fragments/pages).
- `web/security/` : `WebSecurityConfig` (chaîne dédiée : form login, sessions, CSRF),
  `ApplicationUserDetailsService`, `AdminInitializer`.

### Nouveaux domaines (services manquants)
- `utilisateur/` : `Utilisateur`, `Role` (ADMIN/LIVREUR), `UtilisateurRepository`,
  `UtilisateurService`, `UtilisateurDto` (gestion des utilisateurs, étape 11).
- `tournee/TourneeService` : calcul/recalcul de tournée, ordonnancement, marquage arrêt.
- `conversation/ConversationService` : liste + bascule IA/HUMAIN.
- `connaissance/ConnaissanceService` : base de connaissance Markdown + historique + test IA.

### Frontend
- `templates/` : layout (fragments natifs `th:replace`), fragments communs, écrans.
- `static/vendor/htmx.min.js`, `static/vendor/chart.umd.min.js` (servis localement,
  **aucun CDN**), `static/app.css` (CSS à la main, variables CSS), `static/app.js`
  (configuration htmx CSRF + micro-interactions, **aucun script inline**).
- `messages.properties` / `messages_fr.properties` (français).

### Migrations ajoutées
- `V4__utilisateurs.sql`, `V5__connaissances.sql` (production, PostgreSQL).
- Équivalents H2 dans `src/test/resources/db/migration` (tests sans Docker).

## Réutilisation des services existants
Les contrôleurs web ne contiennent **aucune logique métier** : ils délèguent à
`ClientService`, `ProduitService`, `QuartierService`, `CommandeApplicationService`,
`TableauDeBordService`, `StatsService`, `ParametreService`, et aux nouveaux services
créés ci-dessus. Aucune entité JPA n'est liée à un formulaire : DTO + Bean Validation.

## Écarts / risques (voir `PROBLEMES.md`)
- Docker indisponible → tests d'intégration basculés sur H2 (mode PostgreSQL) avec
  migrations de test dédiées.
- Alpine.js écarté (incompatible CSP stricte sans `unsafe-eval`) → micro-interactions
  en JS vanilla externe.
- Tailwind CLI écarté (binaire standalone non intégrable de façon déterministe ici) →
  CSS à la main organisé en variables CSS (repli pré-approuvé).
