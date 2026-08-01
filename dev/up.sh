#!/usr/bin/env bash
# Jedno wejscie do dev stacku ThingsBoard.
#
#   ./dev/up.sh            -> hybrid (Postgres + Cassandra + Kafka + Valkey)
#   ./dev/up.sh minimal    -> minimal (Postgres + Kafka + Valkey), najszybszy start
#   ./dev/up.sh hybrid js  -> hybrid + zdalny tb-js-executor
#
# Pierwsze uruchomienie automatycznie zaklada schemat bazy i dane demo.
set -euo pipefail

cd "$(dirname "$0")"

PROFILE="${1:-hybrid}"
EXTRA_PROFILE="${2:-}"

COMPOSE=(docker compose --env-file .env.dev -f docker-compose.dev.yml)

if [ "$PROFILE" = "minimal" ]; then
  export DATABASE_TS_TYPE=sql
else
  export DATABASE_TS_TYPE=cassandra
fi

echo "==> profil: ${PROFILE} (DATABASE_TS_TYPE=${DATABASE_TS_TYPE})"

echo "==> start infrastruktury"
"${COMPOSE[@]}" --profile "$PROFILE" up -d postgres kafka valkey
if [ "$PROFILE" = "hybrid" ]; then
  "${COMPOSE[@]}" --profile hybrid up -d cassandra
fi

# jednorazowy install schematu + demo

if [ ! -f .installed ]; then
  echo "==> jednorazowy install schematu ThingsBoard (+ dane demo)"
  "${COMPOSE[@]}" --profile install run --rm tb-install
  touch .installed
else
  echo "==> schemat juz zainstalowany (usun dev/.installed aby wymusic ponownie)"
fi

echo "==> start backendu i Angulara"
"${COMPOSE[@]}" --profile "$PROFILE" up -d tb-node-dev tb-ui-dev
if [ -n "$EXTRA_PROFILE" ]; then
  "${COMPOSE[@]}" --profile "$EXTRA_PROFILE" up -d
fi

cat <<EOF

Gotowe.
  UI (Angular dev server) : http://localhost:${UI_PORT:-4200}
  API / backend           : http://localhost:${TB_HTTP_PORT:-8080}
  Remote debug (Java)     : localhost:${TB_DEBUG_PORT:-5005}
  Login demo              : tenant@thingsboard.org / tenant

Logi:      docker compose --env-file dev/.env.dev -f dev/docker-compose.dev.yml logs -f tb-ui-dev tb-node-dev
Restart BE: ./dev/rebuild-backend.sh <modul>
Stop:      docker compose --env-file dev/.env.dev -f dev/docker-compose.dev.yml --profile ${PROFILE} down
EOF
