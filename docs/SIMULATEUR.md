# SIMULATEUR — Simulateur WhatsApp (test de bout en bout)

Interface web à l'aspect WhatsApp pour tester **tout le processus de l'application**. Deux modes :

- **LOCAL** : sans Chatwoot — le message est rejoué en webhook signé, la réponse de l'IA est
  capturée en mémoire.
- **CHATWOOT** : via Chatwoot — le message est créé dans Chatwoot (API publique), Chatwoot
  déclenche le webhook, l'IA répond ; la conversation est alors **visible dans Chatwoot**
  (téléphone compris).

## 1. Chaîne exercée (mode CHATWOOT)

```
Interface /simulateur
        │  POST /simulateur/api/messages (jeton CSRF, session admin)
        ▼
SimulateurService  →  API publique Chatwoot : contact + conversation + message entrant
        │  Chatwoot (worker Sidekiq) déclenche POST /webhooks/chatwoot (signé)
        ▼
ChatwootWebhookController (signature) → ParseurWebhook → WebhookService (idempotence)
        │  TraitementMessageService (bascule IA/humain, création du client)
        │  OrchestrateurIa (DeepSeek) → création de commande
        ▼
ChatwootClient  →  message « outgoing » posté dans la conversation Chatwoot
        ▼
Visible dans Chatwoot (dashboard, téléphone) et relu par l'interface du simulateur
```

## 2. Activation

Le simulateur est désactivé par défaut. Dans `.env.local` (ou l'environnement) :

```
APP_SIMULATEUR_ACTIVE=true
APP_SIMULATEUR_MODE=CHATWOOT     # ou LOCAL
APP_SIMULATEUR_INBOX=<inbox_identifier>   # requis en mode CHATWOOT
```

L'interface est disponible sur `http://localhost:8080/simulateur` (connexion administrateur).

## 3. Prérequis (mode CHATWOOT)

- Chatwoot démarré et initialisé (`bash scripts/start.sh`).
- `CHATWOOT_BASE_URL`, `CHATWOOT_API_TOKEN` (Profil → Access Token) et `CHATWOOT_ACCOUNT_ID`.
- Une boîte de réception **API** créée dans Chatwoot ; `APP_SIMULATEUR_INBOX` = son
  `inbox_identifier` (voir `docs/CHATWOOT_SETUP.md`, section 4.2).
- Le webhook Chatwoot configuré vers l'application (section 4.3).
- `DEEPSEEK_API_KEY` renseigné (sinon pas de réponse IA).

## 4. Utilisation

1. Ouvrir `http://localhost:8080/simulateur` et se connecter en administrateur.
2. Renseigner le **nom** et le **téléphone** du client fictif (numéro international).
3. Envoyer un message (ou utiliser une puce d'exemple).
4. La bulle verte (message client) apparaît, puis la réponse de l'IA (bulle blanche). En mode
   CHATWOOT, la même conversation est visible dans Chatwoot.

Vérifier en parallèle : `tail -f logs/app.log` (webhook reçu, appel IA, commande créée) et
l'écran `/commandes` (la commande créée par l'IA).

## 5. Note technique

- En mode LOCAL, `SimulateurMessagingPort` remplace `ChatwootClient` (voir
  `@ConditionalOnExpression` sur les deux classes) : rien n'est envoyé à Chatwoot.
- En mode CHATWOOT, `ChatwootClient` reste actif (comportement normal) et `SimulateurService`
  crée le contact/conversation via l'API publique, puis lit les réponses via l'API authentifiée.
- Les associations de conversations sont en mémoire (`SimulateurConversationStore`) et perdues au
  redémarrage.
- En mode CHATWOOT, chaque réponse de l'IA est postée avec le jeton d'API : Chatwoot la
  rediffuse en webhook `message_created`. `RegistreMessagesEmis` mémorise le contenu émis juste
  avant l'envoi pour que cet écho ne soit pas confondu avec une prise en main humaine (voir
  § 6).
- Les classes sont couvertes par des tests unitaires (`src/test/java/com/mfish/erp/simulateur`).

## 6. Dépannage (pièges déjà rencontrés)

| Symptôme | Cause | Correction |
| --- | --- | --- |
| `POST /simulateur/api/messages` → **403** | `_csrf` vide dans la page : `SimulateurWebController` est hors du périmètre de `WebControllerAdvice` (`com.mfish.erp.web.controller`) | `SimulateurWebController` expose lui-même le jeton via `@ModelAttribute("csrf")` (`CsrfToken`) |
| Bulle client ok mais **aucune réponse IA** dans l'interface | L'API REST de Chatwoot renvoie `message_type` en **entier** (`0` entrant, `1` sortant, `2` activité) ; filtrer sur la chaîne `"outgoing"` renvoyait une liste vide | `SimulateurService.estSortant` accepte l'entier `1` **et** la chaîne `"outgoing"` (les webhooks utilisent la chaîne) |
| La réponse existe dans Chatwoot mais pas dans le simulateur | Conversation différente (identifiant `sim-…` régénéré à chaque rechargement de page) ou mauvaise `APP_SIMULATEUR_INBOX` | Garder l'onglet ouvert ; vérifier `APP_SIMULATEUR_INBOX` = `inbox_identifier` de l'inbox API |
| **Une seule réponse de l'IA puis plus rien** (bulle `Pas de reponse : verifier DEEPSEEK_API_KEY…` au message suivant) | Chatwoot rediffuse un webhook `message_created` pour les messages **sortants**, y compris ceux que l'application poste avec son jeton d'API : `TraitementMessageService` les prenait pour une prise en main par un agent et basculait la conversation en `HUMAIN` ; l'IA devient alors muette pour tous les messages suivants (aucun appel DeepSeek dans les logs) | `RegistreMessagesEmis` mémorise le contenu émis **avant** l'envoi HTTP ; l'écho webhook correspondant est consommé et ignoré (péremption 10 min). Une réponse d'agent humain (contenu inconnu du registre) bascule toujours en `HUMAIN` |
| Après déploiement, le correctif semble absent | `docker-compose.prod.yml` utilise `pull_policy: always` : l'image CI `ghcr.io/m-aristide/mfish-app:latest` est re-téléchargée | Re-taguer l'image locale puis `docker compose up --pull never --force-recreate --no-deps app` (voir `deploy-simulateur.sh`) |

