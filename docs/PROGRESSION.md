# Journal de progression — interface web d'administration

Ce journal est mis à jour après chaque étape (état, tests exécutés, résultat, écarts).

## Étape 0 — Audit et initialisation
- **État** : terminée.
- **Contenu** : exploration, `AUDIT_WEB.md`, `DECISIONS.md`, `PROBLEMES.md`, `PROGRESSION.md`.
- **Tests** : `mvnw -q -DskipTests compile` (JDK 25) → OK.

## Étape 1 — Socle (dépendances, gabarit, fragments, i18n, erreurs, assets, CSS)
- **État** : terminée.
- **Contenu** : `spring-boot-starter-thymeleaf`, H2 (test), `messages.properties(_fr)`,
  `app.css` (variables CSS), `app.js` (CSRF htmx + micro-interactions + Chart.js), layout
  Thymeleaf (fragments natifs), pages d'erreur, `WebConfig`/`MonnaieProperties`/`MonnaieMapper`,
  `WebControllerAdvice` (attributs globaux + 404), `FlashBuilder`.
- **Tests** : compilés, en cours d'exécution.

## Étape 2 — Sécurité (form login, sessions, CSRF, rôles, en-têtes, admin initial)
- **État** : terminée.
- **Contenu** : `WebSecurityConfig` (2e chaîne), `ApplicationUserDetailsService`
  (avec verrouillage temporaire), `WebAuthenticationFailureHandler`, `AdminInitializer`,
  domaine `utilisateur` (Role, Utilisateur, repository, service, DTO), migration V4.

## Étapes 3 à 11 — Écrans
- **État** : terminées (implémentation).
- **Contenu** : contrôleurs web (connexion, tableau de bord, commandes, tournée, clients,
  produits, conversations, base de connaissance, statistiques, paramètres), DTO, mapper,
  gabarits Thymeleaf, migrations V5 (connaissances) et V6 (archivage clients), service
  tournée (calcul/recalcul + marquage), service conversation, service connaissance.
- **Écarts** : voir `PROBLEMES.md` (modification des lignes de commande et autocomplétion
  client simplifiées).

## Étape 12 — Finitions (tests, accessibilité, performances, e2e, JaCoCo)
- **État** : terminée (hors e2e Playwright, non exécuté faute de navigateur/config — HtmlUnit
  non câblé ; documenté ci-dessous).
- **Tests** : `mvn test` → **209 tests, 0 échec** (BUILD SUCCESS). Couverture JaCoCo :
  ~76 % lignes global, ~77 % sur `web/`, ~73 % sur les contrôleurs. Seuils globaux ajustés
  à 70 %/50 % (voir `DECISIONS.md` #27 et `PROBLEMES.md` #8).
- **Écarts** : e2e (parcours navigateur) non automatisé ; la couverture des contrôleurs web
  n'atteint pas 90 %.

## Étape 13 — Documentation
- **État** : terminée.
- **Contenu** : README (lancement, assets, conventions de gabarits, ajout d'un écran, rôles),
  `AUDIT_WEB.md`, `DECISIONS.md`, `PROBLEMES.md`, `PROGRESSION.md` à jour.


