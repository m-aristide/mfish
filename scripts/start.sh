#!/usr/bin/env bash
# =============================================================================
# start.sh — Démarre PostgreSQL (mfish) puis l'application Spring Boot.
# Charge .env.local (valeurs vides ignorées), écoute en local, attend UP.
# Aucun secret en dur. Idempotent. Compatible Git Bash / WSL / Linux / macOS.
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

is_win() { case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) return 0;; *) return 1;; esac; }

# --- 1. Charger .env.local (ignore commentaires et valeurs vides) ---
if [[ -f .env.local ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"   # Windows CRLF -> LF
    [[ -z "$line" || "$line" == \#* ]] && continue
    if [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)[[:space:]]*=[[:space:]]*(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      val="${BASH_REMATCH[2]}"
      val="${val%$'\r'}"
      val="${val%\"}"; val="${val#\"}"; val="${val%\'}"; val="${val#\'}"
      [[ -n "$val" ]] && export "$key=$val"
    fi
  done < .env.local
fi

# Écoute locale uniquement (aucune exposition vers l'extérieur)
export SERVER_ADDRESS="${SERVER_ADDRESS:-127.0.0.1}"

# --- 2. Base de données ---
docker compose up -d postgres
echo "Attente de la santé de PostgreSQL…"
healthy=""
for _ in $(seq 1 60); do
  healthy="$(docker inspect --format '{{.State.Health.Status}}' mfish-postgres-1 2>/dev/null || true)"
  [[ "$healthy" == "healthy" ]] && break
  sleep 2
done
[[ "$healthy" == "healthy" ]] || { echo "⚠️  PostgreSQL n'est pas sain."; exit 1; }
echo "✓ PostgreSQL sain."

# --- 3. Jar (compilation si absent) ---
JAR="target/erp-0.0.1-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "Jar absent — compilation (tests ignorés)…"; ./mvnw -q -DskipTests package; }

mkdir -p logs
PID_FILE="logs/app.pid"

pid_alive() {
  local pid="$1"
  if is_win; then
    tasklist //FI "PID eq $pid" 2>/dev/null | grep -q "$pid" || return 1
  else
    kill -0 "$pid" 2>/dev/null || return 1
  fi
}

# --- 4. Démarrer l'application (si elle ne tourne pas déjà) ---
if [[ -f "$PID_FILE" ]] && pid_alive "$(cat "$PID_FILE")"; then
  echo "L'application tourne déjà (PID $(cat "$PID_FILE"))."
else
  if is_win; then
    WIN_ROOT="$(cygpath -w "$ROOT")"
    WIN_JAR="$(cygpath -w "$JAR")"
    WIN_ROOT="$WIN_ROOT" WIN_JAR="$WIN_JAR" powershell.exe -NoProfile -Command '$p=Start-Process -FilePath "java" -ArgumentList "-Djavax.net.ssl.trustStoreType=Windows-ROOT","-jar",$env:WIN_JAR -WorkingDirectory $env:WIN_ROOT -RedirectStandardOutput ($env:WIN_ROOT + "\logs\app.log") -RedirectStandardError ($env:WIN_ROOT + "\logs\app.err.log") -WindowStyle Hidden -PassThru; Set-Content -Path ($env:WIN_ROOT + "\logs\app.pid") -Value $p.Id'
    pid="$(cat "$PID_FILE")"
  else
    nohup "${JAVA_BIN:-java}" -jar "$JAR" > logs/app.log 2>&1 &
    pid=$!
    echo "$pid" > "$PID_FILE"
  fi
  echo "Application démarrée (PID $pid) — logs: logs/app.log"
fi

# --- 5. Attendre UP ---
echo "Attente de /actuator/health…"
for _ in $(seq 1 60); do
  code="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/actuator/health 2>/dev/null || true)"
  [[ "$code" == "200" ]] && { echo "✓ UP → http://127.0.0.1:8080"; exit 0; }
  sleep 2
done
echo "⚠️  L'application n'est pas devenue UP à temps — voir logs/app.log"
exit 1
