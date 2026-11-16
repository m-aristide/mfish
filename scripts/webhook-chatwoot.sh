#!/usr/bin/env bash
# =============================================================================
# webhook-chatwoot.sh — Envoie un webhook Chatwoot signé (format officiel) pour
# tester la chaîne de bout en bout sans passer par Chatwoot.
# Signature : sha256=HMAC-SHA256(secret, "<timestamp>.<corps brut>")
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Reprend CHATWOOT_WEBHOOK_SECRET depuis .env.local si la variable n'est pas déjà exportée.
if [[ -z "${CHATWOOT_WEBHOOK_SECRET:-}" && -f "$ROOT/.env.local" ]]; then
  CHATWOOT_WEBHOOK_SECRET="$(sed -n \
    's/^[[:space:]]*CHATWOOT_WEBHOOK_SECRET[[:space:]]*=[[:space:]]*//p' \
    "$ROOT/.env.local" | tail -n1 | tr -d '\r')"
fi

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
SECRET="${CHATWOOT_WEBHOOK_SECRET:-dev-webhook-secret}"
CONVERSATION_ID="${CONVERSATION_ID:-1}"
CONTENU="${CONTENU:-Bonjour, je voudrais commander 2 poulets}"
TELEPHONE="${TELEPHONE:-+237650000001}"
NOM="${NOM:-Client Test}"
ID="${ID:-evt-$(date +%s)}"

corps=$(cat <<JSON
{"event":"message_created","id":"$ID","message_type":"incoming","private":false,"content":"$CONTENU","conversation":{"id":$CONVERSATION_ID},"sender":{"type":"contact","phone_number":"$TELEPHONE","name":"$NOM"}}
JSON
)

horodatage=$(date +%s)
signature="sha256=$(printf '%s' "$horodatage.$corps" \
  | openssl dgst -sha256 -hmac "$SECRET" -r | cut -d' ' -f1)"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/webhooks/chatwoot" \
  -H 'Content-Type: application/json' \
  -H "X-Chatwoot-Timestamp: $horodatage" \
  -H "X-Chatwoot-Signature: $signature" \
  --data-binary "$corps")

echo "POST $BASE_URL/webhooks/chatwoot -> $code"
echo "  événement : $ID (conversation $CONVERSATION_ID, contact $TELEPHONE)"
[[ "$code" == "200" ]] || { echo "⚠️  Webhook refusé — vérifier CHATWOOT_WEBHOOK_SECRET."; exit 1; }
echo "  Suivre le traitement : tail -f logs/app.log"
