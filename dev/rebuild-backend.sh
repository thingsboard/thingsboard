#!/usr/bin/env bash
# Szybka przebudowa backendu BEZ rebuildu obrazu Dockera.
#
#   ./dev/rebuild-backend.sh              # tylko modul application
#   ./dev/rebuild-backend.sh dao          # modul dao + zaleznosci
#   ./dev/rebuild-backend.sh restart      # tylko restart procesu
set -euo pipefail

cd "$(dirname "$0")"
COMPOSE=(docker compose --env-file .env.dev -f docker-compose.dev.yml)
TARGET="${1:-application}"

if [ "$TARGET" = "restart" ]; then
  echo "==> restart tb-node-dev"
  "${COMPOSE[@]}" restart tb-node-dev
  exit 0
fi

echo "==> mvn install -pl ${TARGET} -am (offline, w kontenerze, cache w wolumenie m2-repo)"
"${COMPOSE[@]}" exec tb-node-dev bash -lc "
  cd /src && mvn -o -q -T 1C -DskipTests -Dlicense.skip=true \
    install -pl ${TARGET} -am -Dmaven.compiler.useIncrementalCompilation=true
"

echo "==> spring-boot-devtools podniesie zmiany automatycznie (jesli nie: ./dev/rebuild-backend.sh restart)"
