#!/usr/bin/env bash
# =============================================================================
# deploy-server.sh — Déploie l'application mfish sur le serveur de production.
#
# À exécuter SUR LE SERVEUR (dans /home/ansible/mfish) une fois qu'une nouvelle
# image a été publiée sur ghcr.io. C'est exactement ce que fait automatiquement
# le workflow GitHub Actions ".github/workflows/deploy.yml".
#
# Usage (sur le serveur) :
#   GHCR_PAT='<jeton>' bash deploy-server.sh
#   APP_SERVICE=... COMPOSE_FILE=... bash deploy-server.sh   # surcharges
#
# Variables d'environnement :
#   GHCR_PAT       jeton GitHub (scope read:packages) pour se connecter à ghcr.io
#   GHCR_USER      utilisateur GitHub (défaut m-aristide)
#   APP_SERVICE    nom du service applicatif dans le compose (défaut mfish-app)
#   COMPOSE_FILE   fichier compose (défaut docker-compose.prod.yml)
#   APP_DIR        répertoire du déploiement (défaut /home/ansible/mfish)
#   HEALTH_URL     URL du healthcheck (défaut http://127.0.0.1:8080/actuator/health)
#   MAX_ATTEMPTS   nombre de tentatives de healthcheck (défaut 30)
#   SLEEP          délai entre deux tentatives en secondes (défaut 5)
# =============================================================================
set -euo pipefail

APP_SERVICE="${APP_SERVICE:-mfish-app}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
APP_DIR="${APP_DIR:-/home/ansible/mfish}"
GHCR_USER="${GHCR_USER:-m-aristide}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
SLEEP="${SLEEP:-5}"

cd "$APP_DIR"

if [[ -n "${GHCR_PAT:-}" ]]; then
  echo "$GHCR_PAT" | docker login ghcr.io -u "$GHCR_USER" --password-stdin >/dev/null
fi

echo "Téléchargement de l'image du service $APP_SERVICE ..."
docker compose -f "$COMPOSE_FILE" pull "$APP_SERVICE"

echo "Redémarrage du service $APP_SERVICE ..."
docker compose -f "$COMPOSE_FILE" up -d --no-deps "$APP_SERVICE"

for i in $(seq 1 "$MAX_ATTEMPTS"); do
  code="$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL" || true)"
  if [[ "$code" == "200" ]]; then
    echo "OK : application UP -> $(curl -s "$HEALTH_URL")"
    exit 0
  fi
  sleep "$SLEEP"
done

echo "ECHEC : l'application n'est pas UP après $MAX_ATTEMPTS tentatives."
docker compose -f "$COMPOSE_FILE" logs --tail=200 "$APP_SERVICE"
exit 1
