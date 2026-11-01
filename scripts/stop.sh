#!/usr/bin/env bash
# =============================================================================
# stop.sh — Arrête l'application (via PID) puis la base PostgreSQL.
# Idempotent. Aucun secret. Compatible Git Bash / WSL / Linux / macOS.
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

is_win() { case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) return 0;; *) return 1;; esac; }

PID_FILE="logs/app.pid"
if [[ -f "$PID_FILE" ]]; then
  pid="$(cat "$PID_FILE")"
  if [[ -n "$pid" ]]; then
    if is_win; then
      if taskkill //PID "$pid" //T //F >/dev/null 2>&1; then
        echo "✓ Application arrêtée (PID $pid)."
      else
        echo "Application déjà arrêtée (PID $pid introuvable)."
      fi
    else
      if kill "$pid" 2>/dev/null; then
        echo "✓ Application arrêtée (PID $pid)."
      else
        echo "Application déjà arrêtée (PID $pid introuvable)."
      fi
    fi
  fi
  rm -f "$PID_FILE"
else
  echo "Aucun PID d'application (logs/app.pid absent)."
fi

echo "Arrêt de PostgreSQL…"
docker compose stop postgres
echo "✓ Base arrêtée."
