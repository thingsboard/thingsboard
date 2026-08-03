#!/usr/bin/env bash
# Starts the ThingsBoard backend from the jar produced by the builder container
# and restarts the JVM automatically whenever that jar changes.
set -uo pipefail

JAR=/build/application.jar
PID=""

log() { echo "[tb] $*"; }

resolve_data_dir() {
  # Assembled data dir (produced by mvn package) is preferred; fall back to sources.
  for d in /src/application/target/data /src/application/src/main/data; do
    if [ -d "$d" ]; then echo "$d"; return; fi
  done
  echo ""
}

wait_for_jar() {
  if [ ! -f "$JAR" ]; then
    log "waiting for $JAR — run 'docker compose up builder' to build it"
    while [ ! -f "$JAR" ]; do sleep 5; done
  fi
}

start_jvm() {
  local data_dir debug_opts=()
  data_dir=$(resolve_data_dir)
  if [ "${JAVA_DEBUG:-true}" = "true" ]; then
    debug_opts=("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
  fi

  log "starting ThingsBoard (data_dir=${data_dir:-<none>})"
  # shellcheck disable=SC2086
  java ${JAVA_OPTS:-} "${debug_opts[@]}" \
    -Dlogging.config=/opt/tb/logback.xml \
    -Dinstall.data_dir="${data_dir}" \
    -jar "$JAR" &
  PID=$!
  log "jvm pid=$PID"
}

stop_jvm() {
  if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    log "stopping jvm pid=$PID"
    kill "$PID" 2>/dev/null
    for _ in $(seq 1 30); do
      kill -0 "$PID" 2>/dev/null || break
      sleep 1
    done
    kill -9 "$PID" 2>/dev/null
    wait "$PID" 2>/dev/null
  fi
  PID=""
}

trap 'stop_jvm; exit 0' TERM INT

wait_for_jar
LAST_STAMP=$(stat -c %Y "$JAR")
start_jvm

while true; do
  sleep 3

  # jar replaced by the builder -> hot restart
  if [ -f "$JAR" ]; then
    STAMP=$(stat -c %Y "$JAR")
    if [ "$STAMP" != "$LAST_STAMP" ]; then
      log "new jar detected — restarting backend"
      LAST_STAMP=$STAMP
      stop_jvm
      start_jvm
      continue
    fi
  fi

  # jvm died on its own -> let compose restart policy or our loop recover
  if [ -n "$PID" ] && ! kill -0 "$PID" 2>/dev/null; then
    wait "$PID" 2>/dev/null
    RC=$?
    log "jvm exited (rc=$RC) — restarting in 5s"
    PID=""
    sleep 5
    wait_for_jar
    LAST_STAMP=$(stat -c %Y "$JAR")
    start_jvm
  fi
done
