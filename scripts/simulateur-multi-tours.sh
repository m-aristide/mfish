#!/usr/bin/env bash
# =============================================================================
# simulateur-multi-tours.sh — Echange multi-tours avec l'IA via l'interface du
# simulateur : mêmes appels HTTP que src/main/resources/static/app-simulateur.js
# (connexion administrateur, POST /simulateur/api/messages avec jeton CSRF,
# puis polling de GET /simulateur/api/conversations/{id}/messages).
#
# Le script sert à vérifier qu'une conversation enchaîne plusieurs réponses :
# en mode CHATWOOT, un bug faisait basculer la conversation en HUMAIN dès la
# première réponse de l'IA (voir docs/SIMULATEUR.md, § 6).
#
# Usage :
#   scripts/simulateur-multi-tours.sh
#   BASE_URL=https://mfish.online scripts/simulateur-multi-tours.sh
#
# Variables surchargeables : BASE_URL, ADMIN_USER, ADMIN_PASSWORD, TELEPHONE,
#   NOM, CONVERSATION_ID, DELAI_REPONSE (secondes), INTERVALLE (secondes),
#   MESSAGES (messages séparés par « | »).
# =============================================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
TELEPHONE="${TELEPHONE:-+237690112233}"
NOM="${NOM:-Client Simulateur}"
CONVERSATION_ID="${CONVERSATION_ID:-sim-cli-$(date +%s)}"
DELAI_REPONSE="${DELAI_REPONSE:-60}"
INTERVALLE="${INTERVALLE:-2}"

MESSAGES_DEFAUT="Bonjour, vous avez du poisson silure frais ?|Je voudrais 2 kilos de silure|Livraison le 19/11/2026 a 12h a Obobogo|Pouvez-vous confirmer le montant ?|Merci beaucoup"
IFS='|' read -r -a MESSAGES <<< "${MESSAGES:-$MESSAGES_DEFAUT}"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

pass=0
fail=0
ok() { echo "  ✓ $1"; pass=$((pass + 1)); }
ko() { echo "  ✗ $1"; fail=$((fail + 1)); }

json() { # json <chaine> : echappe pour une valeur de chaine JSON
  printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}

compter() { # compter <corps JSON> : nombre de messages (reponses IA) presents
  grep -o '"contenu"' <<< "$1" | wc -l | tr -d '[:space:]'
}

echo "=== 0. Pré-vol ($BASE_URL) ==="
sante="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/actuator/health" || true)"
if [[ "$sante" != "200" ]]; then
  echo "⚠️  Application injoignable ($BASE_URL) : code $sante."
  exit 1
fi
ok "GET /actuator/health -> 200"

echo "=== 1. Connexion administrateur ==="
curl -s -c "$TMP/cookies.txt" -o "$TMP/connexion.html" "$BASE_URL/connexion"
csrf="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$TMP/connexion.html" | head -n1)"
code="$(curl -s -b "$TMP/cookies.txt" -c "$TMP/cookies.txt" -o /dev/null -w '%{http_code}' \
  -X POST "$BASE_URL/connexion" \
  --data-urlencode "username=$ADMIN_USER" \
  --data-urlencode "password=$ADMIN_PASSWORD" \
  --data-urlencode "_csrf=$csrf")"
if [[ "$code" != "302" ]]; then
  echo "⚠️  Connexion refusée (code $code) : verifier ADMIN_USER / ADMIN_PASSWORD."
  exit 1
fi
ok "connexion administrateur -> 302"

echo "=== 2. Page du simulateur (jeton CSRF) ==="
curl -s -b "$TMP/cookies.txt" -c "$TMP/cookies.txt" -o "$TMP/simulateur.html" "$BASE_URL/simulateur"
csrf_sim="$(sed -n 's/.*name="_csrf" content="\([^"]*\)".*/\1/p' "$TMP/simulateur.html" | head -n1)"
if [[ -z "$csrf_sim" ]]; then
  echo "⚠️  Jeton CSRF absent de la page /simulateur (simulateur desactive ?)."
  exit 1
fi
ok "GET /simulateur -> jeton CSRF recupere"

echo "=== 3. Conversation $CONVERSATION_ID ($NOM / $TELEPHONE) ==="
URL_CONVERSATION="$BASE_URL/simulateur/api/conversations/$CONVERSATION_ID/messages"

for indice in "${!MESSAGES[@]}"; do
  message="${MESSAGES[$indice]}"
  tour=$((indice + 1))
  echo
  echo "--- Tour $tour/${#MESSAGES[@]} : $message"

  avant="$(compter "$(curl -s -b "$TMP/cookies.txt" "$URL_CONVERSATION")")"
  corps="{\"conversationId\":\"$(json "$CONVERSATION_ID")\",\"telephone\":\"$(json "$TELEPHONE")\",\"nom\":\"$(json "$NOM")\",\"contenu\":\"$(json "$message")\"}"
  code="$(curl -s -b "$TMP/cookies.txt" -o "$TMP/envoi.json" -w '%{http_code}' \
    -X POST "$BASE_URL/simulateur/api/messages" \
    -H 'Content-Type: application/json' \
    -H "X-CSRF-TOKEN: $csrf_sim" \
    --data "$corps")"
  if [[ "$code" != "200" ]]; then
    ko "tour $tour : POST /simulateur/api/messages -> $code"
    continue
  fi

  limite=$((SECONDS + DELAI_REPONSE))
  apres="$avant"
  reponses=""
  while (( SECONDS < limite )); do
    sleep "$INTERVALLE"
    reponses="$(curl -s -b "$TMP/cookies.txt" "$URL_CONVERSATION")"
    apres="$(compter "$reponses")"
    if [[ "$apres" -gt "$avant" ]]; then
      break
    fi
  done

  if [[ "$apres" -gt "$avant" ]]; then
    texte="$(sed -n 's/.*"contenu":"\([^"]*\)".*/\1/p' <<< "$reponses" | tail -n1)"
    ok "tour $tour : réponse IA en ${SECONDS}s ($((apres - avant)) message(s))"
    echo "      IA > $texte"
  else
    ko "tour $tour : aucune réponse après ${DELAI_REPONSE}s (bulle « Pas de reponse »)"
    echo "      corps: $reponses"
  fi
done

echo
echo "Résultat : $pass réussi(s), $fail échec(s) — conversation $CONVERSATION_ID"
if [[ "$fail" != "0" ]]; then
  echo "⚠️  Echange incomplet : consulter les logs de l'application et docs/SIMULATEUR.md (§ 6)."
  exit 1
fi
echo "✓ Echange multi-tours valide."
