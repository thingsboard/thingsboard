#!/usr/bin/env bash
# Entrypoint backendu ThingsBoard w trybie dev.
#
#  1. czeka na infrastrukture,
#  2. buduje moduly (pierwszy raz pelny build, potem inkrementalnie),
#  3. startuje watcher zrodel Javy (auto-kompilacja po zapisie pliku),
#  4. uruchamia aplikacje pod supervisorem, ktory restartuje ja na zadanie watchera.
set -uo pipefail

RESTART_FLAG=/tmp/tb-restart-requested
MVN_FLAGS="-T 1C -DskipTests -Dlicense.skip=true -Dpkg.skip=true -Dmaven.javadoc.skip=true -Dgpg.skip=true"

if [ "${TB_DEV_OFFLINE:-true}" = "true" ] && [ -d /root/.m2/repository/org/thingsboard ]; then
  MVN_FLAGS="-o ${MVN_FLAGS}"
fi

log() { echo "[tb-dev] $*"; }

wait_for() {
  local host="$1" port="$2" label="$3" tries=180
  log "czekam na ${label} (${host}:${port})..."
  until nc -z "$host" "$port" 2>/dev/null; do
    tries=$((tries - 1))
    [ "$tries" -le 0 ] && { log "BLAD: ${label} niedostepny"; exit 1; }
    sleep 1
  done
  log "${label} gotowy"
}

wait_for postgres 5432 "PostgreSQL"
[ "${DATABASE_TS_TYPE:-}" = "cassandra" ] && wait_for cassandra 9042 "Cassandra"
wait_for kafka 9092 "Kafka"
wait_for valkey 6379 "Valkey"

cd /src

if [ ! -f /root/.m2/.tb-dev-bootstrapped ]; then
  log "pierwszy build modulow zaleznych - to trwa dlugo (15-40 min), potem juz nigdy"
  # shellcheck disable=SC2086
  mvn ${MVN_FLAGS/-o /} install -DskipTests || { log "BLAD pierwszego builda"; exit 1; }
  touch /root/.m2/.tb-dev-bootstrapped
else
  log "inkrementalny build modulu thingsboard..."
  # shellcheck disable=SC2086
  mvn ${MVN_FLAGS} install -DskipTests -Dmaven.compiler.useIncrementalCompilation=true
fi

# ------------------------------------------------------------------- watcher
rm -f "$RESTART_FLAG"
if [ "${TB_DEV_WATCH:-true}" = "true" ]; then
  /usr/local/bin/tb-node-watch.sh &
  WATCH_PID=$!
  log "watcher zrodel Javy uruchomiony (PID ${WATCH_PID})"
fi

# ---------------------------------------------------------------- supervisor
JVM_ARGS="${TB_JVM_ARGS:--XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss512k -Xmx2g}"
JVM_ARGS="${JVM_ARGS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

start_app() {
  # shellcheck disable=SC2086
  mvn ${MVN_FLAGS} spring-boot:run \
    -Dspring-boot.run.fork=true \
    -Dspring-boot.run.profiles="${TB_SPRING_PROFILES:-dev}" \
    -Dspring-boot.run.jvmArguments="${JVM_ARGS}" &
  APP_PID=$!
}

stop_app() {
  [ -n "${APP_PID:-}" ] || return 0
  kill "$APP_PID" 2>/dev/null || true
  # spring-boot:run forkuje JVM - domykamy potomka
  pkill -f 'org.thingsboard.server.ThingsboardServerApplication' 2>/dev/null || true
  wait "$APP_PID" 2>/dev/null || true
}

trap 'log "zatrzymuje..."; stop_app; kill "${WATCH_PID:-0}" 2>/dev/null || true; exit 0' TERM INT

log "start ThingsBoard (API 8080, debug JDWP 5005)"
start_app

while true; do
  if [ -f "$RESTART_FLAG" ]; then
    rm -f "$RESTART_FLAG"
    log "restart aplikacji po zmianie kodu..."
    stop_app
    start_app
  fi
  # aplikacja padla sama (np. blad startu) - nie zabijamy kontenera, czekamy na poprawke
  if ! kill -0 "${APP_PID:-0}" 2>/dev/null; then
    log "aplikacja zatrzymana. Zapisz plik zrodlowy, zeby wystartowac ponownie."
    while [ ! -f "$RESTART_FLAG" ]; do sleep 2; done
  fi
  sleep 1
done
