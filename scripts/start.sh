#!/usr/bin/env bash
# =============================================================================
# start.sh — Démarre TOUTE la pile mfish en une seule commande :
#   1. PostgreSQL applicatif            (service "postgres")
#   2. PostgreSQL Chatwoot + Redis      (services "chatwoot-postgres", "redis")
#   3. Chatwoot web + worker Sidekiq    (services "chatwoot", "chatwoot-worker")
#   4. Application Spring Boot          (jar recompilé s'il est absent ou périmé)
# Charge .env.local, attend la santé des conteneurs puis l'UP de l'application.
#
# Variables d'environnement acceptées :
#   SERVER_ADDRESS   interface d'écoute de l'app (défaut 127.0.0.1 ;
#                    0.0.0.0 pour être joignable depuis un téléphone)
#   SKIP_CHATWOOT=1  démarre l'infrastructure applicative sans Chatwoot
#   SKIP_APP=1       démarre l'infrastructure sans l'application Spring Boot
#   CHATWOOT_TIMEOUT nombre d'essais (x2 s) pour attendre Chatwoot (défaut 90)
#   JAVA_HOME        JDK 25 (détecté automatiquement s'il est inutilisable)
#
# Arrêt complet : bash scripts/stop.sh
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

# docker compose doit lire .env.local (et non .env)
COMPOSE=(docker compose)
if [[ -f .env.local ]]; then
  COMPOSE+=(--env-file .env.local)
fi

# Écoute locale uniquement par défaut (aucune exposition vers l'extérieur)
export SERVER_ADDRESS="${SERVER_ADDRESS:-127.0.0.1}"

# --- 2. Affichage et aides ---
etape() { printf '\n== %s ==\n' "$1"; }
ok()    { printf '  ✓ %s\n' "$1"; }
attn()  { printf '  ⚠️  %s\n' "$1"; }

# Identifiant du conteneur d'un service (vide si le service n'est pas lancé).
conteneur() { "${COMPOSE[@]}" ps -q "$1" 2>/dev/null | head -n 1 | tr -d '\r'; }

# Attend que le healthcheck d'un service passe à "healthy" (ou "running" sans healthcheck).
attendre_sain() {
  local service="$1" limite="${2:-60}" cid="" statut=""
  for _ in $(seq 1 "$limite"); do
    cid="$(conteneur "$service")"
    if [[ -n "$cid" ]]; then
      statut="$(docker inspect --format \
        '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
        "$cid" 2>/dev/null || true)"
      if [[ "$statut" == "healthy" || "$statut" == "running" ]]; then
        ok "$service ($statut)"
        return 0
      fi
    fi
    sleep 2
  done
  attn "$service n'est pas sain (état : ${statut:-inconnu}) — voir: ${COMPOSE[*]} logs $service"
  return 1
}

# Attend qu'une URL réponde 200.
attendre_http() {
  local url="$1" libelle="$2" limite="${3:-90}" code=""
  for _ in $(seq 1 "$limite"); do
    code="$(curl -s -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || true)"
    if [[ "$code" == "200" ]]; then
      ok "$libelle ($url)"
      return 0
    fi
    sleep 2
  done
  attn "$libelle ne répond pas encore (dernier code : ${code:-aucun})"
  return 1
}

# Version majeure de Java pour un binaire donné (vide si illisible).
major_de_java() { "$1" -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p'; }

# Cherche un JDK >= 25 (le jar est compilé en release 25).
trouver_jdk_25() {
  local candidat
  for candidat in "${JAVA_HOME:-}" \
      "/c/Program Files/Java/jdk-25"* "/c/Program Files/Eclipse Adoptium/jdk-25"* \
      /usr/lib/jvm/*25* /Library/Java/JavaVirtualMachines/*25*/Contents/Home; do
    [[ -n "$candidat" && -x "$candidat/bin/javac" ]] || continue
    if [[ "$(major_de_java "$candidat/bin/java")" -ge 25 ]] 2>/dev/null; then
      printf '%s' "$candidat"
      return 0
    fi
  done
  return 1
}
# --- 3. Pré-requis ---
etape "Pré-requis"
if ! command -v docker >/dev/null 2>&1; then
  attn "Docker est introuvable dans le PATH."
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  attn "Docker Desktop n'est pas démarré — lancez-le puis relancez ce script."
  exit 1
fi
ok "Docker opérationnel"

# --- 4. Bases de données et cache ---
etape "Bases de données et cache"
"${COMPOSE[@]}" up -d postgres chatwoot-postgres redis
for service in postgres chatwoot-postgres redis; do
  attendre_sain "$service" 60
done

# --- 5. Chatwoot (web + worker Sidekiq) ---
etape "Chatwoot"
if [[ "${SKIP_CHATWOOT:-0}" == "1" ]]; then
  attn "Chatwoot ignoré (SKIP_CHATWOOT=1)."
elif [[ -z "${CHATWOOT_SECRET_KEY_BASE:-}" ]]; then
  attn "Chatwoot ignoré : CHATWOOT_SECRET_KEY_BASE est vide dans .env.local."
  attn "Renseignez-le (openssl rand -hex 64) — voir docs/CHATWOOT_SETUP.md, section 3."
else
  # db:chatwoot_prepare n'est lancé que si la base Chatwoot n'a jamais été initialisée.
  schema=""
  for _ in $(seq 1 15); do
    schema="$("${COMPOSE[@]}" exec -T chatwoot-postgres psql -U chatwoot -d chatwoot \
      -tAc "SELECT to_regclass('public.schema_migrations') IS NOT NULL" 2>/dev/null \
      | tr -d '\r ' | head -n 1)"
    [[ "$schema" == "t" || "$schema" == "f" ]] && break
    sleep 2
  done

  if [[ "$schema" == "t" ]]; then
    ok "schéma Chatwoot déjà initialisé"
  else
    echo "  Initialisation du schéma Chatwoot (db:chatwoot_prepare)…"
    echo "  (première exécution : téléchargement de l'image Chatwoot, plusieurs minutes)"
    "${COMPOSE[@]}" run --rm -T chatwoot bundle exec rails db:chatwoot_prepare
    ok "schéma Chatwoot prêt"
  fi

  "${COMPOSE[@]}" up -d chatwoot chatwoot-worker
  attendre_sain chatwoot-worker 30 \
    || attn "Le worker Sidekiq ne tourne pas : Chatwoot n'enverra aucun webhook."
  attendre_http "http://127.0.0.1:3000/api" "Chatwoot" "${CHATWOOT_TIMEOUT:-90}" \
    || attn "Chatwoot démarre encore — suivez: ${COMPOSE[*]} logs -f chatwoot"
fi

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

# Arrête l'application lancée par ce script. Nécessaire avant recompilation :
# sous Windows, le jar en cours d'exécution est verrouillé (renommage impossible).
arreter_app() {
  local pid
  pid="$(tr -d '[:space:]' < "$PID_FILE" 2>/dev/null || true)"
  if [[ -n "$pid" ]] && pid_alive "$pid"; then
    if is_win; then
      taskkill //PID "$pid" //T //F >/dev/null 2>&1 || true
    else
      kill "$pid" 2>/dev/null || true
      for _ in $(seq 1 10); do
        pid_alive "$pid" || break
        sleep 1
      done
      if pid_alive "$pid"; then
        kill -9 "$pid" 2>/dev/null || true
      fi
    fi
    ok "application arrêtée (PID $pid)"
  fi
  rm -f "$PID_FILE"
}

APP_EN_COURS="non"
if [[ -f "$PID_FILE" ]] && pid_alive "$(tr -d '[:space:]' < "$PID_FILE")"; then
  APP_EN_COURS="oui"
fi

# --- 6. Jar de l'application (recompilation si absent ou obsolète) ---
JAR="target/erp-0.0.1-SNAPSHOT.jar"
# Le jar Spring Boot est produit avec un horodatage figé (build reproductible, 1980) :
# on compare donc les sources à un marqueur écrit après chaque compilation réussie.
BUILD_MARKER="target/.build-ok"

# Vrai si le jar est absent, tronqué, ou plus ancien qu'une source. Un jar périmé
# provoque par exemple un "Migration checksum mismatch" au démarrage.
jar_a_recompiler() {
  if [[ ! -f "$JAR" || ! -f "$BUILD_MARKER" ]]; then
    return 0
  fi
  if [[ "$(wc -c < "$JAR" 2>/dev/null || echo 0)" -lt 1000000 ]]; then
    return 0
  fi
  [[ -n "$(find src pom.xml -newer "$BUILD_MARKER" -print -quit 2>/dev/null)" ]]
}

if [[ "${SKIP_APP:-0}" != "1" ]] && jar_a_recompiler; then
  etape "Compilation"
  if [[ "$APP_EN_COURS" == "oui" ]]; then
    attn "L'application tourne avec l'ancien jar : arrêt avant recompilation."
    arreter_app
    APP_EN_COURS="non"
  fi
  if jdk25="$(trouver_jdk_25)"; then
    export JAVA_HOME="$jdk25"
    ok "JAVA_HOME=$JAVA_HOME"
  else
    attn "Aucun JDK 25 détecté : la compilation (release 25) risque d'échouer."
  fi
  echo "  Compilation (tests ignorés)…"
  ./mvnw -q -DskipTests package
  touch "$BUILD_MARKER"
fi

# --- 7. Application Spring Boot (si elle ne tourne pas déjà) ---
APP_DEMARREE="non"
if [[ "${SKIP_APP:-0}" == "1" ]]; then
  attn "Application ignorée (SKIP_APP=1)."
elif [[ "$APP_EN_COURS" == "oui" ]]; then
  ok "application déjà lancée (PID $(tr -d '[:space:]' < "$PID_FILE"))"
  APP_DEMARREE="oui"
else
  etape "Application"
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
  ok "application lancée (PID $pid) — logs: logs/app.log"
  APP_DEMARREE="oui"
fi

# --- 8. Attente UP ---
if [[ "$APP_DEMARREE" == "oui" ]]; then
  echo "  Attente de /actuator/health…"
  if ! attendre_http "http://127.0.0.1:8080/actuator/health" "application" 90; then
    attn "L'application n'est pas UP — voir logs/app.log"
    exit 1
  fi
fi

# --- 9. Résumé ---
etape "Pile mfish démarrée"
echo "  Application : http://127.0.0.1:8080   (SERVER_ADDRESS=$SERVER_ADDRESS)"
if [[ "$SERVER_ADDRESS" == "0.0.0.0" ]]; then
  echo "  Réseau local : http://<IP-DU-PC>:8080  (téléphone, même Wi-Fi)"
fi
if [[ "${SKIP_CHATWOOT:-0}" != "1" && -n "${CHATWOOT_SECRET_KEY_BASE:-}" ]]; then
  echo "  Chatwoot    : ${CHATWOOT_FRONTEND_URL:-http://localhost:3000}  (API sur le port 3000)"
fi
echo "  Logs        : logs/app.log"
echo "  État        : bash scripts/status.sh"
echo "  Arrêt       : bash scripts/stop.sh"
