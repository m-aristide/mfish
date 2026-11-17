# Architecture

Application de gestion de livraison pour petit commerçant, construite avec **Java 25**,
**Spring Boot 4.1.2** (Spring Framework 7, Jakarta EE 11, Jackson 3, Hibernate 7).

## Vue d'ensemble

```
Client WhatsApp → WhatsApp Business API (Meta Cloud)
   → Chatwoot (boîte partagée, le commerçant garde la main)
   → Webhook → APPLICATION SPRING BOOT
        ├─ Module IA (API DeepSeek)
        ├─ Module commandes (PostgreSQL)
        └─ Module tournée (API RouteXL)
   → Interface de gestion (PWA) lisant la même base
```

## Principes

- **Architecture hexagonale (ports & adaptateurs)** : le domaine ne dépend jamais de
  l'infrastructure. Les appels externes passent par des interfaces (ports) implémentées par des
  adaptateurs (Chatwoot, DeepSeek, RouteXL, géocodage).
- **Organisation par domaine** (pas par couche technique globale), sous `com.mfish.erp` :
  `commande`, `client`, `produit`, `quartier`, `tournee`, `conversation`, `ia`, `webhook`,
  `notification`, `stats`, `securite`, `infrastructure`, `config`.
- **Injection par constructeur uniquement** (pas de `@Autowired` sur champ).
- **DTO en `record`**, entités JPA jamais exposées par l'API.
- **Montants en entiers** (`long`, plus petite unité monétaire), jamais de `double`/`float`.
- **Horloge injectable** (`java.time.Clock`), jamais d'appel direct à `Instant.now()`.

## Ports et adaptateurs

| Port (interface) | Adaptateur | Usage |
|---|---|---|
| `MessagingPort` | `ChatwootClient` | lire/envoyer messages, étiqueter conversations |
| `LlmPort` | `DeepSeekClient` | analyser un message, extraire la commande, rédiger la réponse |
| `RoutingPort` | `RouteXlClient` | optimiser l'ordre des arrêts, heures d'arrivée |
| `GeocodingPort` | adaptateur (Nominatim) | adresse/quartier → coordonnées GPS |
| `Clock` | bean injecté | horloge testable |

Chaque adaptateur gère : timeout, retry avec backoff (Spring Retry), exceptions typées,
journalisation sans fuite de clé API.

## Flux principaux

1. **Prise de commande IA** : webhook Chatwoot → `WebhookController` (signature, idempotence,
   filtrage) → traitement asynchrone → `LlmPort` (DeepSeek) → validation de la sortie
   (produits rapprochés du catalogue, prix lus en base, quartier validé) → commande créée si
   `complet = true`.
2. **Tournée** : commandes du jour → géocodage (repli quartier) → `RoutingPort` (RouteXL) →
   plan B local (tri `ordre_defaut` + plus proche voisin, distance de Haversine) → validation.
3. **Notification** : après validation, message par client (fenêtre ±30 min), règle des 24 h
   WhatsApp, suivi anti-doublon en base.

## Sécurité

- JWT stateless HS256 (Nimbus JOSE JWT 10, via `spring-security-oauth2-jose`), filtres Spring
  Security, `BCryptPasswordEncoder` (via `DelegatingPasswordEncoder`).
- CSRF désactivé sur `/api/**`, CORS restreint, rate limiting sur le login, en-têtes de
  sécurité.

## Base de données

- PostgreSQL 16, schéma géré uniquement par **Flyway** (`db/migration`),
  `spring.jpa.hibernate.ddl-auto=validate`.
- Entités : clients, quartiers, produits, commandes, lignes_commande, tournees,
  arrets_tournee, conversations, evenements_traites, parametres, evenements_log.

## Observabilité

- Spring Boot Actuator (`/actuator/health`, probes), logs structurés (ECS en prod).

## Frontend

- PWA React + Vite + TypeScript dans `web/` (mobile-first, mode hors-ligne).
