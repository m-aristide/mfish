# Rapport de déploiement — mfish sur mfish.online

**Date** : 2026-11-13
**Cible** : `https://mfish.online/` (application) + `https://chat.mfish.online/` (Chatwoot)
**Serveur** : `mfish.online` → `100.42.179.74` (Contabo, hostname `vmi2039668.contaboserver.net`)

---

## 1. Résumé

L'application Spring Boot **mfish** (ERM de livraison) et son instance **Chatwoot** dédiée ont
été déployées sur le serveur `mfish.online` via SSH, derrière le reverse-proxy **nginx**
(conteneur Docker `webapp`) qui écoute sur les ports 80/443. Le pare-feu **UFW** a été activé.

| Élément | URL | État |
|---|---|---|
| Application mfish (interface web) | `https://mfish.online/` | ✅ UP |
| Health check | `https://mfish.online/actuator/health` | ✅ `{"status":"UP"}` |
| Chatwoot (dédié mfish) | `https://chat.mfish.online/` | ✅ accessible (compte `MFISH`) |
| Pare-feu UFW | — | ✅ actif (22, 80, 443, 587) |

---

## 2. État des lieux initial du serveur

- **OS** : Ubuntu 20.04.6 LTS.
- **Docker** : 28.0.1, **Docker Compose** : v2.33.1.
- **Utilisateur SSH** : `ansible` (groupes `sudo` et `docker`).
- **UFW** : `inactive` (règles préexistantes : `22`, `587/tcp`).
- **Docker daemon.json** :
  ```json
  { "insecure-registries": ["warap.cm:5000"], "iptables": false }
  ```
- **Stack existant** (ne pas casser) : microservices `warap` + nginx `webapp` (80/443) +
  un Chatwoot **existant** (`chatwoot-rails-1` :3000, `chatwoot-postgres-1` :5432,
  `chatwoot-redis-1` :6379).
- **Disque** : `/dev/sda3` 1.2 To, **96 % plein** (~50 Go libres).
- **DNS** : `mfish.online` et `www.mfish.online` → `100.42.179.74`.
  `chat.mfish.online` a été ajouté (enregistrement A) pendant le déploiement.
- `.env.prod` ne contenait **pas** `APP_JWT_SECRET`, `APP_ADMIN_PASSWORD_HASH`,
  `CHATWOOT_WEBHOOK_SECRET` (requis en prod, fail-fast).

---

## 3. Problèmes rencontrés et corrections

1. **Conflit de clé d'hôte SSH** (entrée IP `100.42.179.74` obsolète dans `known_hosts`).
   → `ssh-keygen -R 100.42.179.74`.

2. **Conteneurs Docker sans accès Internet** (cause racine, en deux volets).
   `daemon.json` contient `"iptables": false` → Docker ne crée **ni** règle NAT
   (`MASQUERADE`) **ni** règle `FORWARD ACCEPT` pour les ponts Docker.
   a. Ajout d'une règle **MASQUERADE** ciblée sur les sous-réseaux Docker
      (`-A POSTROUTING -s 172.16.0.0/12 -j MASQUERADE`), persistée via
      `iptables-persistent`.
   b. Le `MASQUERADE` seul est **insuffisant** : UFW est en `deny (routed)` et
      aucune règle `FORWARD` n'autorisait les conteneurs. Ajout de
      `ufw route allow from 172.16.0.0/12` (persisté dans `/etc/ufw/user.rules`).
   **Impact constaté** : sans (b), DNS externe et egress TCP/443 échouaient dans les
   conteneurs (Chatwoot ne pouvait donc pas livrer son webhook vers
   `https://mfish.online`). Vérifié après correctif : DNS + TCP/443 OK depuis
   `mfish-app` et `mfish-chatwoot` ; `GET https://mfish.online/actuator/health` = `200`
   depuis Chatwoot **et** son worker.

3. **Build Maven côté serveur instable** (après la correction DNS, échec d'extraction
   `tar: Cannot open: Function not implemented`).
   → Pivot : **compilation du jar en local** (JDK 25) puis image runtime minimale
   (`eclipse-temurin:25-jre` + jar), sans build Maven côté serveur.

4. **Variables d'environnement manquantes** (fail-fast au démarrage) :
   `APP_JWT_SECRET`, `APP_ADMIN_PASSWORD_HASH`, `CHATWOOT_WEBHOOK_SECRET`.
   → Générées et injectées dans `.env` (voir §6 et §7).

5. **`$` du hash bcrypt interprété par docker compose** comme une variable.
   → Échappement en `$$` dans le fichier `.env`.

6. **Conflits de ports** avec le Chatwoot existant (3000, 5432, 6379).
   → Le stack mfish utilise des ports internes uniquement (Postgres/Redis non publiés) ;
   Chatwoot mfish publié sur `127.0.0.1:3001` ; l'app sur `127.0.0.1:8080` (localhost).

7. **`ParseurWebhook` ne correspondait pas au payload webhook réel de Chatwoot** (cause racine
   du test de bout en bout muet). Chatwoot expose les données du contact sous **`sender`**
   (`sender.webhook_data` → `sender.phone_number`, `sender.name`), **pas** sous une clé
   `contact` au sommet. Le parseur lisait `contact.phone_number`/`contact.name` (toujours nuls
   dans le payload réel), donc `TraitementMessageService` ignorait silencieusement chaque
   message entrant (`contactPhone == null` → return).
   → Correction : lecture de `sender.phone_number`/`sender.name` avec repli sur `contact`
   (pour les scripts locaux/tests), test unitaire ajouté (`ParseurWebhookTest`), jar
   recompilé (JDK 25) et redéployé. Validé de bout en bout (voir §8).

---

## 4. Fichiers déployés sur le serveur (`/home/ansible/mfish/`)

| Fichier | Rôle |
|---|---|
| `docker-compose.prod.yml` | Compose de production (6 services, réseau externe `docker_web-net`) |
| `Dockerfile.runtime` | Image runtime minimale (JRE 25 + jar, utilisateur non-root `mfish`) |
| `.env` | Variables d'environnement de production (secrets) |
| `erp-0.0.1-SNAPSHOT.jar` | Jar Spring Boot compilé localement (69 116 894 octets) |
| `nginx.conf` | Copié vers `/home/ansible/docker/nginx.conf` (sauvegarde `.bak.mfish`) |

Le `docker-compose.prod.yml` définit 6 services (tous `restart: unless-stopped`) :

| Service | Image | Port host | Réseaux |
|---|---|---|---|
| `mfish-app` | `mfish-app:1.0.0` | `127.0.0.1:8080` | default + docker_web-net |
| `mfish-chatwoot` | `chatwoot/chatwoot:latest` | `127.0.0.1:3001` | default + docker_web-net |
| `mfish-chatwoot-worker` | `chatwoot/chatwoot:latest` | — | default |
| `mfish-postgres` | `pgvector/pgvector:pg16` | — | default |
| `mfish-chatwoot-postgres` | `pgvector/pgvector:pg16` | — | default |
| `mfish-redis` | `redis:7-alpine` | — | default |

Les services `app`, `chatwoot` et `chatwoot-worker` reçoivent `dns: [8.8.8.8, 1.1.1.1]`
(pour les appels externes : DeepSeek, RouteXL, géocodage).

---

## 5. Commandes exécutées sur le serveur (chronologie)

### 5.1. Correction clé d'hôte SSH (depuis la machine locale)
```bash
ssh-keygen -R 100.42.179.74 -f ~/.ssh/known_hosts
```

### 5.2. Correction du NAT sortant Docker (cause racine)
```bash
# Vérifier le forwarding IP
sysctl net.ipv4.ip_forward                       # => 1

# Autoriser le NAT sortant pour les sous-réseaux Docker (172.16.0.0/12)
sudo iptables -t nat -A POSTROUTING -s 172.16.0.0/12 -j MASQUERADE

# Autoriser le FORWARD (UFW "deny routed" ; iptables:false => pas de règle Docker)
#   persisté dans /etc/ufw/user.rules
sudo ufw route allow from 172.16.0.0/12

# Rendre la règle MASQUERADE persistante (survit au reboot)
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y iptables-persistent netfilter-persistent
sudo netfilter-persistent save
```

### 5.3. Génération des secrets
```bash
# (équivalent de ce qui a été exécuté)
openssl rand -hex 48                     # APP_JWT_SECRET
openssl rand -base64 18 | tr -d '/+=' | head -c 16   # mot de passe admin
docker run --rm httpd:alpine htpasswd -bnBC 10 "" "$ADMIN_PASS" | tr -d ':\n'   # hash bcrypt
openssl rand -hex 32                     # CHATWOOT_WEBHOOK_SECRET
```

### 5.4. Construction de l'image runtime
```bash
cd /home/ansible/mfish
docker build -f Dockerfile.runtime -t mfish-app:1.0.0 .
```

### 5.5. Initialisation et démarrage du stack
```bash
cd /home/ansible/mfish
# 1. Bases + cache
docker compose -f docker-compose.prod.yml up -d postgres chatwoot-postgres redis
# 2. Initialisation du schéma Chatwoot (une seule fois)
docker compose -f docker-compose.prod.yml run --rm chatwoot bundle exec rails db:chatwoot_prepare
# 3. Démarrage complet
docker compose -f docker-compose.prod.yml up -d
```

### 5.6. Reverse-proxy nginx (conteneur `webapp`)
```bash
# Sauvegarde
sudo cp -a /home/ansible/docker/nginx.conf /home/ansible/docker/nginx.conf.bak.mfish
# Déploiement de la nouvelle config
sudo cp /home/ansible/mfish/nginx.conf /home/ansible/docker/nginx.conf
# Test + rechargement
docker exec webapp nginx -t
docker exec webapp nginx -s reload
```

### 5.7. Certificat SSL pour Chatwoot
```bash
docker exec webapp certbot --nginx -d chat.mfish.online \
  --non-interactive --agree-tos --redirect --no-eff-email
```

### 5.8. Pare-feu UFW
```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable
sudo ufw status verbose
```

---

## 6. Configuration finale

### 6.1. UFW
```
Status: active
Default: deny (incoming), allow (outgoing), deny (routed)

22/tcp     ALLOW   Anywhere      # SSH
80/tcp     ALLOW   Anywhere      # HTTP  (nginx)
443/tcp    ALLOW   Anywhere      # HTTPS (nginx)
587/tcp    ALLOW   Anywhere      # SMTP submission (règle préexistante)
```

> `ufw route allow from 172.16.0.0/12` autorise le **forwarding** des conteneurs
> (`-A ufw-user-forward -s 172.16.0.0/12 -j ACCEPT`). Sans elle, `deny (routed)`
> bloque tout egress : DNS et HTTPS externes. Règle persistée dans
> `/etc/ufw/user.rules`.

### 6.2. nginx (`/home/ansible/docker/nginx.conf`, extraits)
```nginx
# mfish.online -> application (port 8080)
server {
  server_name mfish.online;
  underscores_in_headers on;
  location / {
    proxy_pass_header Authorization;
    proxy_pass http://mfish-app:8080;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Ssl on;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_http_version 1.1;
    proxy_buffering off;
    client_max_body_size 0;
    proxy_read_timeout 36000s;
    proxy_redirect off;
  }
  listen 443 ssl;
  ssl_certificate     /etc/letsencrypt/live/mfish.online/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/mfish.online/privkey.pem;
  include /etc/letsencrypt/options-ssl-nginx.conf;
  ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

# chat.mfish.online -> Chatwoot (port 3000)
server {
  listen 443 ssl;
  server_name chat.mfish.online;
  location / {
    proxy_pass_header Authorization;
    proxy_pass http://mfish-chatwoot:3000;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Ssl on;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_http_version 1.1;
    proxy_buffering off;
    client_max_body_size 0;
    proxy_read_timeout 36000s;
    proxy_redirect off;
  }
  ssl_certificate     /etc/letsencrypt/live/chat.mfish.online/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/chat.mfish.online/privkey.pem;
  include /etc/letsencrypt/options-ssl-nginx.conf;
  ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}
```

### 6.3. Variables d'environnement (`.env`)
Valeurs de `.env.prod` utilisées (DeepSeek, RouteXL, Postgres, Chatwoot, géocodage,
timezone) **plus** les secrets générés :
`APP_JWT_SECRET`, `APP_ADMIN_PASSWORD_HASH`, `CHATWOOT_WEBHOOK_SECRET`,
`CHATWOOT_FRONTEND_URL=https://chat.mfish.online`,
`CHATWOOT_BASE_URL=http://chatwoot:3000`,
`CHATWOOT_ACCOUNT_ID=1`,
`CHATWOOT_API_TOKEN=c5Ziz6wgDqes5FDAcUUiCeDM`,
`APP_CORS_ALLOWED_ORIGINS=https://mfish.online`,
ainsi que le **SMTP** (relais Gmail) pour les e-mails Chatwoot (invitations d'agents,
notifications) : `MAILER_SENDER_EMAIL=arismasminc@gmail.com`, `SMTP_DOMAIN=mfish.online`,
`SMTP_ADDRESS=smtp.gmail.com`, `SMTP_PORT=587`, `SMTP_USERNAME=arismasminc@gmail.com`,
`SMTP_PASSWORD` (mot de passe d'application Gmail), `SMTP_AUTHENTICATION=plain`,
`SMTP_ENABLE_STARTTLS_AUTO=true`, `SMTP_OPENSSL_VERIFY_MODE=peer`.

> ⚠️ Le hash bcrypt contient des `$` ; ils sont échappés en `$$` dans `.env`
> pour ne pas être interprétés par docker compose.

---

## 7. Identifiants (sensibles)

| Élément | Valeur |
|---|---|
| URL application | `https://mfish.online/` |
| Utilisateur admin | `admin` |
| **Mot de passe admin** | `Z58XrIFhlJeZ5KJe` |
| `APP_JWT_SECRET` | `244e7539fd828d4cfe5e882dd4409054648066eb30be5056f0f13402df5a565e34bab3cc092053b378b425dcc7f3e883` |
| `CHATWOOT_WEBHOOK_SECRET` | `e2dd004d96add2272cf3e4448868c90f30c84907580895130d52028b9efcca62` |
| `CHATWOOT_ACCOUNT_ID` | `1` (compte `MFISH`) |
| `CHATWOOT_API_TOKEN` | `c5Ziz6wgDqes5FDAcUUiCeDM` |
| URL Chatwoot | `https://chat.mfish.online/` (compte admin `MFISH` créé) |

> 🔐 **À faire** : changer le mot de passe admin de l'application (le compte
> administrateur Chatwoot `MFISH` est déjà créé).

---

## 8. Tests effectués (résultat)

| Test | Résultat |
|---|---|
| `GET https://mfish.online/actuator/health` | ✅ `200` `{"status":"UP"}` |
| `GET https://mfish.online/connexion` | ✅ `200` (page de connexion) |
| `GET https://mfish.online/` | ✅ `302` → `/connexion` |
| `GET https://chat.mfish.online/` | ✅ `200` (login/dashboard après onboarding) |
| `GET https://chat.mfish.online/app/login` | ✅ `200` |
| Conteneurs mfish (6) | ✅ tous `Up` (Postgres/Redis `healthy`) |
| `nginx -t` | ✅ syntaxe OK |
| `GET https://chat.mfish.online/api/v1/profile` (token API) | ✅ `200` |
| `POST https://mfish.online/webhooks/chatwoot` (sans signature) | ✅ `401` |
| `POST https://mfish.online/webhooks/chatwoot` (signé HMAC) | ✅ `200` |
| DNS externe depuis `mfish-app` (`api.deepseek.com`) | ✅ résolu |
| DNS + TCP/443 externes depuis `mfish-chatwoot` | ✅ OK |
| `GET https://mfish.online/actuator/health` depuis Chatwoot/worker | ✅ `200` |
| Boîte API + message entrant → webhook → IA → réponse `outgoing` | ✅ testé de bout en bout |
| Envoi SMTP (relais Gmail) via `ActionMailer` | ✅ `EMAIL_ENVOYE_OK` |

---

## 9. Limites connues et recommandations

1. **Intégration Chatwoot ↔ app finalisée et validée de bout en bout.** Compte admin `MFISH`
   (account id `1`), `CHATWOOT_API_TOKEN`/`CHATWOOT_ACCOUNT_ID` injectés, webhook Chatwoot
   (`https://mfish.online/webhooks/chatwoot`) configuré avec le secret partagé. Une boîte de
   réception **API** `WhatsApp test` (`inbox_identifier 8ujHRcdoF3BY5hEWt3UFgBGT`) a été créée
   pour tester sans compte Meta : un message entrant a déclenché webhook → IA (DeepSeek) →
   réponse `outgoing` dans la conversation. **Reste à faire** : créer une vraie boîte
   **WhatsApp** (voir `docs/WHATSAPP_SETUP.md`) pour la production. Voir
   `docs/CHATWOOT_SETUP.md`.

2. **Répertoire d'upload non persistant.** `app.stockage.uploads-dir=uploads` pointe sur
   `/app/uploads` dans le conteneur (perdu si le conteneur est recréé). Pour persister les
   images produits, ajouter un volume nommé monté sur `/app/uploads` (avec le bon uid 10001).

3. **Disque à 96 %.** Penser à nettoyer les images Docker inutilisées
   (`docker system prune`) et les logs (`cron_output.log` ≈ 47 Mo dans `/home/ansible`).

4. **Ports legacy désormais fermés.** L'activation d'UFW bloque l'accès externe direct aux
   ports 3000 (Chatwoot legacy), 5432 (Postgres legacy) et 6379 (Redis legacy). Ces services
   restent joignables via les réseaux Docker et nginx. Si un accès externe direct est requis,
   le rouvrir avec `sudo ufw allow <port>/tcp`.

5. **Renouvellement TLS.** Le cron existant `docker exec webapp certbot renew` renouvelle les
   certificats (dont `chat.mfish.online`, expiration 2027-02-11).

6. **Redémarrage du daemon Docker à éviter sans précautions.** `daemon.json` reste en
   `"iptables": false` ; la règle MASQUERADE est désormais persistée, mais tout redémarrage
   du daemon Docker relancera les conteneurs (vérifier les politiques `restart`).
