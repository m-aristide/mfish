# Déploiement continu (CD) — mfish sur mfish.online

Ce document décrit la mise en place du déploiement automatique : quand la branche
`develop` est fusionnée dans `main` via une pull request, l'application est
reconstruite puis redéployée automatiquement sur `mfish.online`.

## Principe

```
PR develop -> main (fusion)
        │
        ▼
 GitHub Actions (.github/workflows/deploy.yml)
   1. build image Docker (Dockerfile multi-étapes JDK 25 -> JRE 25)
   2. push image sur ghcr.io (ghcr.io/m-aristide/mfish-app:latest + :<sha>)
   3. SSH vers mfish.online
      - docker login ghcr.io (PAT read:packages)
      - docker compose pull mfish-app
      - docker compose up -d --no-deps mfish-app
      - healthcheck /actuator/health (200)
```

Le build Maven a lieu **dans GitHub Actions** (et non sur le serveur), ce qui
supprime l'instabilité du build Maven côté serveur constatée lors du premier
déploiement manuel (voir `DEPLOIEMENT_mfish.online.md`, §3.3).

---

## 1. Prérequis : secrets GitHub à configurer

Aller dans **GitHub → repo `m-aristide/mfish` → Settings → Secrets and variables
→ Actions → New repository secret** et ajouter :

| Secret | Valeur |
|---|---|
| `SSH_HOST` | `mfish.online` (ou `100.42.179.74`) |
| `SSH_USER` | `ansible` |
| `SSH_PRIVATE_KEY` | **contenu** de la clé privée locale `C:\Users\PC\.ssh\warapprod` |
| `GH_PAT` | la valeur de `GITHUB_KEY` du fichier `.env.prod` |

> **Obtention de `SSH_PRIVATE_KEY`** (contenu du fichier, collé tel quel,
> en incluant `-----BEGIN ... PRIVATE KEY-----` et la ligne de fin) :
> ```powershell
> Get-Content C:\Users\PC\.ssh\warapprod -Raw
> ```

> **Scope du `GH_PAT`** : le jeton doit disposer au minimum du scope
> **`read:packages`** (pour que le serveur puisse tirer l'image privée depuis
> ghcr.io). Il est possible de restreindre ce jeton au seul dépôt `mfish`.

---

## 2. Prérequis ponctuel : modification du `docker-compose.prod.yml` (une seule fois)

Le serveur utilise aujourd'hui une image construite localement
(`mfish-app:1.0.0`). Pour que le CD fonctionne, le service applicatif doit
pointer vers l'image publiée sur ghcr.io.

Sur le serveur (`ssh ansible@mfish.online`), éditer
`/home/ansible/mfish/docker-compose.prod.yml` et, **uniquement** dans le service
`mfish-app`, remplacer la construction locale par l'image du registre :

```yaml
  mfish-app:
    image: ghcr.io/m-aristide/mfish-app:latest
    pull_policy: always
    # build: .                    # <- supprimer / commenter
    # dockerfile: Dockerfile.runtime
```

> ⚠️ Ne pas modifier les autres services, les volumes nommés ni le fichier
> `.env` : ils contiennent les données (PostgreSQL, Chatwoot, Redis) et les
> secrets de production. Seul le service `mfish-app` est concerné.

Vérifier la syntaxe avant de continuer :

```bash
cd /home/ansible/mfish
docker compose -f docker-compose.prod.yml config >/dev/null && echo "compose OK"
```

---

## 3. Fonctionnement

- **Déclenchement automatique** : toute pull request fusionnée dans `main`
  (dont `develop -> main`) déclenche le workflow `Déploiement (CD)`.
- **Déclenchement manuel** : onglet **Actions → Déploiement (CD) → Run workflow**.
- Le déploiement utilise l'image taguée `:latest` (l'image `:<sha>` est aussi
  publiée, utile pour identifier/rejouer un déploiement précis).

### Restreindre au seul merge de `develop`

Par défaut, tout merge de PR vers `main` déclenche le déploiement. Pour le
limiter strictement à `develop -> main`, décommenter la condition dans
`.github/workflows/deploy.yml` :

```yaml
if: github.event_name == 'workflow_dispatch' || (github.event.pull_request.merged == true && github.event.pull_request.head.ref == 'develop')
```

---

## 4. Déploiement manuel (secours)

En cas de besoin, déployer à la main depuis le serveur :

```bash
cd /home/ansible/mfish
GHCR_PAT='<jeton>' bash deploy-server.sh
```

(Le script `scripts/deploy-server.sh` fait la même chose que le workflow :
login ghcr.io → pull → up → healthcheck.)

---

## 5. Dépannage

| Symptôme | Cause probable / action |
|---|---|
| `docker login ghcr.io` échoue | Scope `read:packages` manquant sur le `GH_PAT` |
| `pull` échoue (image introuvable / accès refusé) | L'image est privée et le jeton n'a pas accès au paquet `mfish` |
| L'application reste DOWN | `docker compose -f docker-compose.prod.yml logs --tail=200 mfish-app` |
| L'image ne se met pas à jour | Vérifier `pull_policy: always` et que `build:` est bien supprimé dans `mfish-app` |
| Disque plein (serveur à ~96 %) | `docker system prune -af` puis nettoyer `/home/ansible/cron_output.log` |

> **Rollback** : chaque déploiement publie aussi une image taguée `:<sha>`.
> Pour revenir à une version antérieure, éditer temporairement
> `image: ghcr.io/m-aristide/mfish-app:<sha>` dans le service `mfish-app` puis
> `docker compose -f docker-compose.prod.yml up -d --no-deps mfish-app`.
