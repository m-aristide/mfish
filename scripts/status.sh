#!/usr/bin/env bash
# =============================================================================
# status.sh — État de la base, de l'application, derniers logs.
# Aucun secret. Compatible Git Bash / WSL / Linux / macOS.
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

is_win() { case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) return 0;; *) return 1;; esac; }

echo "=== PostgreSQL ==="
docker compose ps postgres 2>/dev/null || true

echo
echo "=== Application ==="
if [[ -f logs/app.pid ]]; then
  pid="$(cat logs/app.pid)"
  alive="non"
  if is_win; then
    tasklist //FI "PID eq $pid" 2>/dev/null | grep -q "$pid" && alive="oui"
  else
    kill -0 "$pid" 2>/dev/null && alive="oui"
  fi
  echo "PID $pid : en cours = $alive"
else
  echo "Pas de PID (logs/app.pid absent)."
fi
echo "Santé : $(curl -s http://127.0.0.1:8080/actuator/health 2>/dev/null || echo 'injoignable')"

echo
echo "=== Derniers logs (logs/app.log) ==="
tail -n 20 logs/app.log 2>/dev/null || echo "(aucun log)"
