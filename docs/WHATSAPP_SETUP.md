# Configuration WhatsApp Cloud API (Meta)

Guide pas à pas pour connecter l'application à l'API officielle WhatsApp Business
Platform (Cloud API). Les valeurs des identifiants ne sont **jamais** stockées dans le
dépôt : uniquement des variables d'environnement (voir `.env.example`).

## 1. Créer l'application Meta et récupérer les identifiants

1. Aller sur <https://developers.facebook.com/apps> et créer une application de type
   **Business**.
2. Ajouter le produit **WhatsApp**.
3. Récupérer dans **Meta for Developers → WhatsApp → Configuration de l'API** les
   valeurs à reporter dans `.env.local` (uniquement les noms, jamais les valeurs en clair
   dans ce dépôt) :

| Variable d'environnement | Où la trouver |
|---|---|
| `WHATSAPP_ACCESS_TOKEN` | Jeton d'accès (temporaire dans l'écran de test, ou permanent via un utilisateur système) |
| `WHATSAPP_PHONE_NUMBER_ID` | Identifiant du numéro de téléphone professionnel (ex. `106540352242922`) |
| `WHATSAPP_BUSINESS_ACCOUNT_ID` | Identifiant du WhatsApp Business Account (WABA) |
| `WHATSAPP_APP_SECRET` | Secret de l'application Meta (Paramètres → Général → Secret de l'application) |
| `WHATSAPP_VERIFY_TOKEN` | Jeton **choisi par vous** (chaîne aléatoire longue), utilisé pour la vérification GET du webhook |
| `WHATSAPP_TEST_RECIPIENT` | Numéro de test unique autorisé pendant la mission (ex. `+2250700000001`) |
| `WHATSAPP_API_VERSION` | Version de l'API Graph (défaut `v21.0`, configurable) |

Activer le module : `WHATSAPP_ENABLED=true`.

## 2. Enregistrer le webhook

Le webhook est exposé sur :

```
POST /webhooks/whatsapp   (réception des messages et statuts)
GET  /webhooks/whatsapp   (vérification)
```

Dans **Meta for Developers → WhatsApp → Configuration → Webhooks** :

1. Cliquer **Modifier** puis **Configurer un webhook**.
2. **URL de rappel** : `https://<votre-domaine-public>/webhooks/whatsapp`
   (l'URL doit être **HTTPS**, fournie par le tunnel ngrok/Cloudflare en local).
3. **Jeton de vérification** : la valeur de `WHATSAPP_VERIFY_TOKEN`.
4. Cliquer **Vérifier et enregistrer** (Meta envoie un `GET` avec `hub.mode=subscribe`,
   `hub.verify_token` et `hub.challenge` ; l'application renvoie `hub.challenge`).
5. Abonner les **champs** de webhook : `messages` (obligatoire), et éventuellement
   `message_template_status_update` pour le statut des templates.

> L'enregistrement peut aussi être fait par l'API si le jeton et le secret le permettent
> (abonnement du champ `messages` sur l'application/WABA). À défaut, saisir l'URL et le
> token dans la console Meta comme décrit ci-dessus.

## 3. Jeton temporaire vs jeton permanent

- **Jeton temporaire** (test) : valable ~24 h, fourni dans l'écran de test. Suffisant
  pour valider l'intégration.
- **Jeton permanent (utilisateur système)** : créer un **utilisateur système** dans
  **Business Manager → Utilisateurs système**, lui attribuer l'application et la
  permission `whatsapp_business_messaging` / `whatsapp_business_management`, puis générer
  un jeton longue durée. Utiliser ce jeton en production (jamais un jeton de test).

## 4. Passage en production

1. **Vérification de l'entreprise** (Business Manager) : documents légaux, coordonnées.
2. **Numéro réel** : ajouter un vrai numéro WhatsApp (non utilisé par un compte WhatsApp
   personnel) et le faire vérifier.
3. **Qualité du numéro** : surveiller la note dans WhatsApp Manager (répondre vite,
   éviter les blocages/spams).
4. **Limites de messages** : quota progressif (nombre de conversations initiées par
   l'entreprise sur 24 h), augmenté avec la qualité du numéro.

## 5. Règles métier à connaître

- **Fenêtre de service de 24 h** : un message libre n'est autorisé que dans les 24 h
  suivant le dernier message du client. Au-delà, **template obligatoire**.
- **Opt-in** : ne contacter que des clients ayant consenti.
- **Opt-out** : gérer `STOP`, `ARRÊT`, désinscription → marquer le client non
  contactable et ne plus envoyer de messages proactifs.
- **Catégories de templates** : `utility` (transactionnel), `marketing`, `authentication`.
  Les modèles marketing exigent l'opt-in et passent par une API dédiée.

## 6. Dépannage

| Symptôme | Cause probable | Action |
|---|---|---|
| Webhook non reçu | URL non enregistrée, tunnel éteint, champ non abonné | Vérifier l'enregistrement et le tunnel HTTPS |
| Signature rejetée (401) | `WHATSAPP_APP_SECRET` incorrect ou corps modifié | Vérifier le secret ; ne pas modifier le corps brut |
| Erreur `131030` | Destinataire non autorisé en mode test | Ajouter le numéro aux destinataires de test Meta |
| Jeton expiré (code `190`) | Jeton temporaire périmé | Régénérer ou passer à un jeton permanent |
| Hors fenêtre 24 h (code `131047`) | Message libre hors fenêtre | Utiliser un template approuvé |
