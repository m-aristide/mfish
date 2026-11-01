#!/usr/bin/env bash
# =============================================================================
# smoke-test.sh — Rejoue les vérifications de l'étape 5 (sans appels externes).
# Les appels réels DeepSeek/RouteXL sont activés par l'option --with-external.
# Utilise le compte administrateur de développement (admin / admin123 par défaut,
# surchargeable via ADMIN_USER / ADMIN_PASSWORD). Aucun secret de production.
# =============================================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
WITH_EXTERNAL=0
[[ "${1:-}" == "--with-external" ]] && WITH_EXTERNAL=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

pass=0; fail=0
ok() { echo "  ✓ $1"; pass=$((pass+1)); }
ko() { echo "  ✗ $1"; fail=$((fail+1)); }

check() { # check <description> <attendu> <reçu>
  if [[ "$3" == "$2" ]]; then ok "$1"; else ko "$1 (attendu=$2 reçu=$3)"; fi
}

echo "=== 1. Santé ==="
health="$(curl -s -w '\n%{http_code}' "$BASE_URL/actuator/health")"
check "GET /actuator/health → 200" "200" "$(echo "$health" | tail -n1)"

echo "=== 2. Page de connexion ==="
code="$(curl -s -o "$TMP/login.html" -w '%{http_code}' "$BASE_URL/connexion")"
check "GET /connexion → 200" "200" "$code"
grep -q 'name="username"' "$TMP/login.html" && ok "formulaire présent" || ko "formulaire absent"

echo "=== 3. Redirection de sécurité ==="
code="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/")"
check "page protégée → 302" "302" "$code"

echo "=== 4. Connexion administrateur ==="
curl -s -c "$TMP/cookies.txt" -o "$TMP/login.html" "$BASE_URL/connexion"
csrf="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$TMP/login.html" | head -n1)"
code="$(curl -s -b "$TMP/cookies.txt" -c "$TMP/cookies.txt" -o /dev/null -w '%{http_code}' \
  -X POST "$BASE_URL/connexion" \
  --data-urlencode "username=$ADMIN_USER" \
  --data-urlencode "password=$ADMIN_PASSWORD" \
  --data-urlencode "_csrf=$csrf")"
check "POST /connexion → 302" "302" "$code"

echo "=== 5. Lecture en base (page protégée) ==="
code="$(curl -s -b "$TMP/cookies.txt" -o "$TMP/produits.html" -w '%{http_code}' "$BASE_URL/produits")"
check "GET /produits (authentifié) → 200" "200" "$code"

if [[ "$WITH_EXTERNAL" == "1" ]]; then
  echo "=== 6. DeepSeek (appel réel, UN SEUL) ==="
  curl -s -b "$TMP/cookies.txt" -c "$TMP/cookies.txt" -o "$TMP/kb.html" "$BASE_URL/connaissance"
  csrf2="$(sed -n 's/.*name="_csrf" content="\([^"]*\)".*/\1/p' "$TMP/kb.html" | head -n1)"
  code="$(curl -s -b "$TMP/cookies.txt" -o "$TMP/ia.html" -w '%{http_code}' \
    -X POST "$BASE_URL/connaissance/test" \
    --data-urlencode "question=Quels sont vos horaires ?" \
    --data-urlencode "_csrf=$csrf2")"
  check "POST /connaissance/test → 200" "200" "$code"

  echo "=== 7. RouteXL (appel réel, UN SEUL) ==="
  curl -s -b "$TMP/cookies.txt" -c "$TMP/cookies.txt" -o "$TMP/tournee.html" "$BASE_URL/tournee"
  csrf3="$(sed -n 's/.*name="_csrf" content="\([^"]*\)".*/\1/p' "$TMP/tournee.html" | head -n1)"
  code="$(curl -s -b "$TMP/cookies.txt" -o /dev/null -w '%{http_code}' \
    -X POST "$BASE_URL/tournee/calculer" --data-urlencode "_csrf=$csrf3")"
  check "POST /tournee/calculer → 302" "302" "$code"
fi

echo
echo "Résultat : $pass réussi(s), $fail échec(s)"
[[ "$fail" == "0" ]]
