#!/usr/bin/env bash
# =============================================================================
# stop.sh — Arrête TOUTE la pile mfish :
#   1. l'application Spring Boot (via logs/app.pid)
#   2. le worker Chatwoot puis Chatwoot web
#   3. Redis et les deux PostgreSQL (applicatifs + Chatwoot)
#
# Usage :
#   bash scripts/stop.sh             arrête tout (conteneurs et données conservés)
#   bash scripts/stop.sh --down      arrête et supprime les conteneurs (données conservées)
#   bash scripts/stop.sh --volumes   idem --down puis SUPPRIME LES DONNÉES (destructif)
#
# Idempotent. Aucun secret. Compatible Git Bash / WSL / Linux / macOS.
# Démarrage complet : bash scripts/start.sh
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

is_win() { case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) return 0;; *) return 1;; esac; }

usage() {
  sed -n '3,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

MODE="stop"
for arg in "$@"; do
  case "$arg" in
    --down)       MODE="down" ;;
    --volumes|-v) MODE="volumes" ;;
    -h|--help)    usage; exit 0 ;;
    *)            echo "Option inconnue : $arg (--help pour l'aide)"; exit 1 ;;
  esac
done

# docker compose doit lire .env.local (et non .env)
COMPOSE=(docker compose)
if [[ -f .env.local ]]; then
  COMPOSE+=(--env-file .env.local)
fi

etape() { printf '\n== %s ==\n' "$1"; }
ok()    { printf '  ✓ %s\n' "$1"; }
attn()  { printf '  ⚠️  %s\n' "$1"; }

# --- 1. Application Spring Boot ---
etape "Application"
PID_FILE="logs/app.pid"
if [[ -f "$PID_FILE" ]]; then
  pid="$(tr -d '[:space:]' < "$PID_FILE")"
  if [[ -n "$pid" ]]; then
    if is_win; then
      if taskkill //PID "$pid" //T //F >/dev/null 2>&1; then
        ok "application arrêtée (PID $pid)"
      else
        attn "application déjà arrêtée (PID $pid introuvable)"
      fi
    else
      if kill "$pid" 2>/dev/null; then
        for _ in $(seq 1 10); do
          kill -0 "$pid" 2>/dev/null || break
          sleep 1
        done
        if kill -0 "$pid" 2>/dev/null; then
          kill -9 "$pid" 2>/dev/null || true
        fi
        ok "application arrêtée (PID $pid)"
      else
        attn "application déjà arrêtée (PID $pid introuvable)"
      fi
    fi
  fi
  rm -f "$PID_FILE"
else
  ok "aucune application lancée par start.sh (logs/app.pid absent)"
fi

# --- 2. Conteneurs (app conteneurisée, worker, web, bases et cache) ---
if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
  attn "Docker n'est pas démarré : les conteneurs sont déjà arrêtés."
  exit 0
fi

SERVICES=(app chatwoot-worker chatwoot chatwoot-postgres redis postgres)

case "$MODE" in
  stop)
    etape "Conteneurs"
    "${COMPOSE[@]}" stop "${SERVICES[@]}" >/dev/null 2>&1 || true
    ok "conteneurs arrêtés (données conservées)"
    ;;
  down)
    etape "Conteneurs"
    "${COMPOSE[@]}" down >/dev/null 2>&1 || true
    ok "conteneurs supprimés (volumes conservés)"
    ;;
  volumes)
    etape "Conteneurs et données"
    attn "Suppression de TOUTES les données (PostgreSQL mfish, Chatwoot, Redis, storage)…"
    "${COMPOSE[@]}" down -v >/dev/null 2>&1 || true
    ok "conteneurs et volumes supprimés"
    ;;
esac

etape "Pile mfish arrêtée"
echo "  Redémarrage : bash scripts/start.sh"
