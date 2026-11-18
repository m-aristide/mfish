#!/usr/bin/env bash
# =============================================================================
# scenario-e2e.sh — Scénario client de bout en bout sur une instance mfish :
#   conversation WhatsApp (webhook Chatwoot signé) -> réponse IA -> client +
#   commande -> calcul de la tournée du jour de livraison.
#
# Scénario rejoué (voir docs/SCENARIO_TEST_E2E.md) :
#   1. « Vous avez du poisson silure frais ? »              -> réponse, commande nulle
#   2. « 3 kg, livraison le 19/11/2026 à 12h à Obobogo »    -> client + commande
#   3. « Calculer la tournée » du 19/11/2026                -> 1 arrêt à 08:02
#
# Prérequis :
#   - application démarrée (scripts/start.sh) ;
#   - DEEPSEEK_API_KEY valide : l'IA est réellement appelée (aucun stub ici) ;
#   - PostgreSQL accessible via docker exec (DB_CONTAINER).
#
# Usage :
#   scripts/scenario-e2e.sh                 # date par défaut : 2026-11-19
#   scripts/scenario-e2e.sh 2026-11-20
#
# Variables surchargeables : BASE_URL, ADMIN_USER, ADMIN_PASSWORD, TELEPHONE,
#   NOM, QUARTIER, PRODUIT, QUANTITE, CONVERSATION_ID, HEURE_ATTENDUE,
#   DB_CONTAINER, DB_USER, DB_NAME.
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"

DATE_LIVRAISON="${1:-${DATE_LIVRAISON:-2026-11-19}}"
CONVERSATION_ID="${CONVERSATION_ID:-999999}"
TELEPHONE="${TELEPHONE:-+237690112233}"
NOM="${NOM:-Awa Ndongo}"
QUARTIER="${QUARTIER:-Obobogo}"
PRODUIT="${PRODUIT:-poisson silure frais}"
QUANTITE="${QUANTITE:-3}"
ADRESSE="${ADRESSE:-rue 1.234 maison 12}"
HEURE_ATTENDUE="${HEURE_ATTENDUE:-08:02}"

DB_CONTAINER="${DB_CONTAINER:-mfish-postgres-1}"
DB_USER="${DB_USER:-${POSTGRES_USER:-mfish}}"
DB_NAME="${DB_NAME:-${POSTGRES_DB:-mfish}}"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

pass=0
fail=0
ok() { echo "  ✓ $1"; pass=$((pass + 1)); }
ko() { echo "  ✗ $1"; fail=$((fail + 1)); }

check() { # check <description> <attendu> <reçu>
  if [[ "$3" == "$2" ]]; then ok "$1"; else ko "$1 (attendu=$2 reçu=$3)"; fi
}

sql() { # sql <requete> : execute une requete et retourne la premiere valeur
  docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -tAc "$1" 2>/dev/null | tr -d '\r' | head -n1
}

sql_exec() { # sql_exec <script sql> : execute un script sans resultat
  docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -q -c "$1" >/dev/null
}

joli_date() { # joli_date 2026-11-19 -> 19/11/2026
  echo "${1:8:2}/${1:5:2}/${1:0:4}"
}

echo "=== 0. Pré-vol ($BASE_URL) ==="
sante="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/actuator/health" || true)"
check "GET /actuator/health -> 200" "200" "$sante"
if [[ "$sante" != "200" ]]; then
  echo "⚠️  Application injoignable : démarrer l'application (scripts/start.sh)."
  exit 1
fi
if ! docker exec -i "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
  echo "⚠️  Base injoignable : conteneur $DB_CONTAINER / utilisateur $DB_USER."
  exit 1
fi
ok "PostgreSQL joignable ($DB_CONTAINER/$DB_NAME)"

echo "=== 1. Remise à zéro du scénario ==="
sql_exec "
DELETE FROM arrets_tournee
 WHERE commande_id IN (SELECT c.id FROM commandes c
                         JOIN clients cl ON cl.id = c.client_id
                        WHERE cl.telephone = '$TELEPHONE')
    OR tournee_id IN (SELECT id FROM tournees WHERE date = DATE '$DATE_LIVRAISON');
DELETE FROM tournees WHERE date = DATE '$DATE_LIVRAISON';
DELETE FROM lignes_commande
 WHERE commande_id IN (SELECT c.id FROM commandes c
                         JOIN clients cl ON cl.id = c.client_id
                        WHERE cl.telephone = '$TELEPHONE');
DELETE FROM commandes
 WHERE client_id IN (SELECT id FROM clients WHERE telephone = '$TELEPHONE');
DELETE FROM clients WHERE telephone = '$TELEPHONE';
DELETE FROM conversations WHERE chatwoot_conversation_id = '$CONVERSATION_ID';
"
ok "données d'une exécution précédente supprimées"

prix_unitaire="$(sql "SELECT prix FROM produits WHERE lower(nom) = lower('$PRODUIT') AND actif ORDER BY nom LIMIT 1")"
if [[ -z "$prix_unitaire" ]]; then
  echo "⚠️  Produit « $PRODUIT » absent du catalogue : l'IA refusera la commande."
  exit 1
fi
montant_attendu=$(( QUANTITE * prix_unitaire ))
ok "produit au catalogue : $PRODUIT ($prix_unitaire FCFA / unité)"

if [[ "$(sql "SELECT count(*) FROM quartiers WHERE actif AND lower(nom) = lower('$QUARTIER')")" == "0" ]]; then
  echo "⚠️  Quartier « $QUARTIER » absent du référentiel (voir docs/SCENARIO_TEST_E2E.md §8.1)."
  echo "    Créer le quartier dans l'écran « Quartiers », ou relancer avec un quartier existant :"
  echo "    QUARTIER=\"Obobogo Rail\" $0 $DATE_LIVRAISON"
  exit 1
fi
ok "quartier au référentiel : $QUARTIER"

echo "=== 2. Conversation WhatsApp (webhooks Chatwoot signés) ==="
HORODATAGE="$(date +%s)"
ID_1="evt-scenario-$HORODATAGE-1"
ID_2="evt-scenario-$HORODATAGE-2"

if BASE_URL="$BASE_URL" CONVERSATION_ID="$CONVERSATION_ID" TELEPHONE="$TELEPHONE" \
   NOM="$NOM" ID="$ID_1" \
   CONTENU="Bonjour, est-ce que vous avez du $PRODUIT ?" \
   bash "$ROOT/scripts/webhook-chatwoot.sh" >/dev/null; then
  ok "question envoyée ($ID_1)"
else
  ko "question refusée par le webhook (vérifier CHATWOOT_WEBHOOK_SECRET)"
  exit 1
fi

if BASE_URL="$BASE_URL" CONVERSATION_ID="$CONVERSATION_ID" TELEPHONE="$TELEPHONE" \
   NOM="$NOM" ID="$ID_2" \
   CONTENU="Je prends $QUANTITE kilos de $PRODUIT, livraison le $(joli_date "$DATE_LIVRAISON") a 12h a $QUARTIER, $ADRESSE." \
   bash "$ROOT/scripts/webhook-chatwoot.sh" >/dev/null; then
  ok "commande envoyée ($ID_2)"
else
  ko "commande refusée par le webhook"
  exit 1
fi

echo "=== 3. Traitement IA (attente de la commande) ==="
client_id=""
for _ in $(seq 1 45); do
  client_id="$(sql "SELECT id FROM clients WHERE telephone = '$TELEPHONE'")"
  if [[ -n "$client_id" ]]; then
    if [[ "$(sql "SELECT count(*) FROM commandes WHERE client_id = '$client_id'")" != "0" ]]; then
      break
    fi
  fi
  sleep 2
done
if [[ -z "$client_id" ]]; then
  ko "client non créé par le webhook (voir logs/app.log)"
  echo; echo "Résultat : $pass réussi(s), $((fail + 1)) échec(s)"; exit 1
fi
ok "client créé ($client_id)"

echo "=== 4. Vérifications de la commande ==="
check "E1 nom du client" "$NOM" \
  "$(sql "SELECT nom FROM clients WHERE telephone = '$TELEPHONE'")"
check "E2 quartier reconnu par l'IA" "$QUARTIER" \
  "$(sql "SELECT q.nom FROM clients cl JOIN quartiers q ON q.id = cl.quartier_id
           WHERE cl.telephone = '$TELEPHONE'")"
check "E3 date de livraison souhaitee" "$DATE_LIVRAISON" \
  "$(sql "SELECT to_char(c.date_livraison_souhaitee, 'YYYY-MM-DD')
            FROM commandes c JOIN clients cl ON cl.id = c.client_id
           WHERE cl.telephone = '$TELEPHONE'")"
check "E4 montant total ($QUANTITE x $prix_unitaire FCFA)" "$montant_attendu" \
  "$(sql "SELECT c.montant_total FROM commandes c JOIN clients cl ON cl.id = c.client_id
           WHERE cl.telephone = '$TELEPHONE'")"
check "E5 source de la commande" "WHATSAPP_IA" \
  "$(sql "SELECT c.source FROM commandes c JOIN clients cl ON cl.id = c.client_id
           WHERE cl.telephone = '$TELEPHONE'")"
check "E6 statut avant tournee" "A_PLANIFIER" \
  "$(sql "SELECT c.statut FROM commandes c JOIN clients cl ON cl.id = c.client_id
           WHERE cl.telephone = '$TELEPHONE'")"
check "E7 quantite commandee" "$QUANTITE" \
  "$(sql "SELECT l.quantite FROM lignes_commande l
            JOIN commandes c ON c.id = l.commande_id
            JOIN clients cl ON cl.id = c.client_id
           WHERE cl.telephone = '$TELEPHONE'")"

echo "=== 5. Calcul de la tournée du $(joli_date "$DATE_LIVRAISON") ==="
curl -s -c "$TMP/cookies.txt" -o "$TMP/connexion.html" "$BASE_URL/connexion"
csrf="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$TMP/connexion.html" | head -n1)"
code="$(curl -s -b "$TMP/cookies.txt" -c "$TMP/cookies.txt" -o /dev/null -w '%{http_code}' \
  -X POST "$BASE_URL/connexion" \
  --data-urlencode "username=$ADMIN_USER" \
  --data-urlencode "password=$ADMIN_PASSWORD" \
  --data-urlencode "_csrf=$csrf")"
check "connexion administrateur -> 302" "302" "$code"

curl -s -b "$TMP/cookies.txt" -c "$TMP/cookies.txt" -o "$TMP/tournee.html" \
  "$BASE_URL/tournee?date=$DATE_LIVRAISON"
csrf_tournee="$(sed -n 's/.*name="_csrf" content="\([^"]*\)".*/\1/p' "$TMP/tournee.html" | head -n1)"
code="$(curl -s -b "$TMP/cookies.txt" -o /dev/null -w '%{http_code}' \
  -X POST "$BASE_URL/tournee/calculer" \
  -H "X-CSRF-TOKEN: $csrf_tournee" \
  --data-urlencode "date=$DATE_LIVRAISON")"
check "POST /tournee/calculer -> 302" "302" "$code"

check "E8 tournee validee" "VALIDEE" \
  "$(sql "SELECT statut FROM tournees WHERE date = DATE '$DATE_LIVRAISON'")"
nb_arrets="$(sql "SELECT count(*) FROM arrets_tournee a
                   JOIN tournees t ON t.id = a.tournee_id
                  WHERE t.date = DATE '$DATE_LIVRAISON'")"
check "E9 nombre d'arrets de la tournee" "1" "$nb_arrets"
check "E10 commande planifiee" "PLANIFIEE" \
  "$(sql "SELECT c.statut FROM commandes c JOIN clients cl ON cl.id = c.client_id
           WHERE cl.telephone = '$TELEPHONE'")"
heure="$(sql "SELECT to_char(a.heure_estimee, 'HH24:MI') FROM arrets_tournee a
               JOIN tournees t ON t.id = a.tournee_id
               JOIN commandes c ON c.id = a.commande_id
               JOIN clients cl ON cl.id = c.client_id
              WHERE t.date = DATE '$DATE_LIVRAISON' AND cl.telephone = '$TELEPHONE'")"
if [[ "$nb_arrets" == "1" ]]; then
  check "E11 heure d'arrivee estimee (depart 08:00)" "$HEURE_ATTENDUE" "$heure"
else
  if [[ -n "$heure" ]]; then
    ok "E11 heure d'arrivee estimee ($heure, tournee multi-arrets)"
  else
    ko "E11 heure d'arrivee estimee absente"
  fi
fi

echo
echo "Résultat : $pass réussi(s), $fail échec(s)"
echo "Client $NOM ($TELEPHONE) — commande du $(joli_date "$DATE_LIVRAISON") — $montant_attendu FCFA"
if [[ "$fail" != "0" ]]; then
  echo "⚠️  Scénario en échec : consulter logs/app.log et docs/SCENARIO_TEST_E2E.md."
  exit 1
fi
echo "✓ Scénario complet validé."
