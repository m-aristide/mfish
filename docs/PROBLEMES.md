# Problèmes et contournements

Ce document consigne les blocages rencontrés, les causes supposées, les contournements
appliqués et le travail restant.

## 1. Docker indisponible (Testcontainers PostgreSQL)
- **Symptôme** : `docker version` échoue (`npipe:////./pipe/dockerDesktopLinuxEngine`),
  le daemon Docker Desktop n'est pas démarré. Les tests `@SpringBootTest` / `@DataJpaTest`
  qui importent `TestcontainersConfiguration` ne peuvent pas démarrer de conteneur PostgreSQL.
- **Cause supposée** : Docker Desktop non lancé / non installé dans cet environnement.
- **Contournement** : bascule des tests d'intégration sur **H2** (mode PostgreSQL) avec des
  migrations de test dédiées (`src/test/resources/db/migration`, dialecte H2) et
  `spring.jpa.hibernate.ddl-auto=none`. Le bean PostgreSQL de `TestcontainersConfiguration`
  est conditionné à la disponibilité de Docker (réutilisable quand Docker sera disponible).
- **Travail restant** : relancer la suite Testcontainers (PostgreSQL réel) sur une machine
  avec Docker pour valider le parcours commande → tournée → livraison → encaissement sur le
  moteur réel.

## 2. JaCoCo : seuils globaux vs nouveaux modules web
- **Symptôme** : les seuils JaCoCo du `pom.xml` sont globaux (90 % lignes / 85 % branches)
  et ont été calibrés avec les tests d'intégration PostgreSQL (qui ne tournent pas sans
  Docker). L'ajout du module `web/` abaisse la couverture globale.
- **Contournement** : couverture ciblée des contrôleurs web (≥ 90 %) et du package `web/`
  (≥ 80 %) via `@WebMvcTest` sans base. L'écart éventuel sur le seuil global est documenté
  ici (voir `docs/PROGRESSION.md`).
- **Travail restant** : après restauration de Docker, réconcilier les seuils globaux.

## 3. Alpine.js incompatible avec la CSP stricte
- **Symptôme** : Alpine.js évalue les expressions via `new Function`, ce qui nécessite
  `script-src 'unsafe-eval'`, incompatible avec la CSP stricte exigée (pas de script inline,
  pas d'eval).
- **Contournement** : Alpine écarté ; micro-interactions réalisées avec htmx + un module
  JS vanilla externe (`static/app.js`). Documenté dans `DECISIONS.md`.
- **Travail restant** : néant (décision définitive).
