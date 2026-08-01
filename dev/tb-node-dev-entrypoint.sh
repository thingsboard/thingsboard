#!/usr/bin/env bash
# Entrypoint backendu w trybie dev.
# 1. czeka na baze/kolejke, 2. buduje modul application inkrementalnie, 3. odpala spring-boot:run
set -euo pipefail

MVN_FLAGS="-q -T 1C -DskipTests -Dlicense.skip=true -Dmaven.javadoc.skip=true -Dgpg.skip=true"
if [ "${TB_DEV_OFFLINE:-true}" = "true" ] && [ -d /root/.m2/repository/org/thingsboard ]; then
  # tryb offline: Maven nie odpytuje Centrala -> start krotszy o kilkadziesiat sekund
  MVN_FLAGS="-o ${MVN_FLAGS}"
fi

wait_for() {
  local host="$1" port="$2" label="$3" tries=120
  echo "[tb-dev] czekam na ${label} (${host}:${port})..."
  until nc -z "$host" "$port" 2>/dev/null; do
    tries=$((tries - 1))
    [ "$tries" -le 0 ] && { echo "[tb-dev] BLAD: ${label} niedostepny"; exit 1; }
    sleep 1
  done
  echo "[tb-dev] ${label} gotowy"
}

wait_for postgres 5432 "PostgreSQL"
if [ "${DATABASE_TS_TYPE:-}" = "cassandra" ]; then
  wait_for cassandra 9042 "Cassandra"
fi
wait_for kafka 9092 "Kafka"
wait_for valkey 6379 "Valkey"

cd /src

# Pierwsze uruchomienie: pelny build modulow zaleznych (jednorazowo, potem z cache w wolumenie).
if [ ! -f /root/.m2/.tb-dev-bootstrapped ]; then
  echo "[tb-dev] pierwszy build modulow zaleznych (jednorazowo, dlugo)..."
  mvn ${MVN_FLAGS/-o /} install -pl application -am
  touch /root/.m2/.tb-dev-bootstrapped
else
  echo "[tb-dev] inkrementalny build modulu application..."
  mvn ${MVN_FLAGS} install -pl application -am -Dmaven.compiler.useIncrementalCompilation=true
fi

echo "[tb-dev] start ThingsBoard (hot-reload wlaczony, debug na 5005)"
exec mvn ${MVN_FLAGS} -pl application spring-boot:run \
  -Dspring-boot.run.fork=false \
  -Dspring-boot.run.profiles=dev
