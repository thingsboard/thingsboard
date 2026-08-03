#!/usr/bin/env bash
# ThingsBoard dev builder.
#
#   watch-build.sh full   -> full build of the whole monorepo (first run, 20-40 min)
#   watch-build.sh once   -> incremental build of the application module only
#   watch-build.sh watch  -> incremental build, then rebuild on every source change
#
# The resulting boot jar is published as /build/application.jar. The backend
# container watches that file and restarts itself whenever it changes.
set -uo pipefail

SRC=/src
OUT=/build/application.jar
STAMP=/build/.full-build-done
MODE="${1:-watch}"
INTERVAL="${WATCH_INTERVAL:-2}"

log() { echo "[builder] $*"; }

publish_jar() {
  local jar
  jar=$(find "$SRC/application/target" -maxdepth 1 -name '*boot.jar' -print -quit 2>/dev/null)
  if [ -z "$jar" ]; then
    jar=$(find "$SRC/application/target" -maxdepth 1 -name 'thingsboard-*.jar' \
            ! -name '*sources*' ! -name '*javadoc*' -print -quit 2>/dev/null)
  fi
  if [ -z "$jar" ]; then
    log "ERROR: no boot jar found in application/target — build failed?"
    return 1
  fi
  cp -f "$jar" "$OUT.tmp" && mv -f "$OUT.tmp" "$OUT"
  log "published $(basename "$jar") -> $OUT"
}

# Offline mode makes incremental builds much faster, but a newly added
# dependency in a pom.xml must be downloaded, so we go online when a pom changed.
build_incremental() {
  local offline="-o"
  if [ "${POM_CHANGED:-0}" = "1" ] || [ ! -f "$STAMP" ]; then
    offline=""
  fi
  log "incremental build (mvn ${offline:-online} -pl application -am)"
  mvn $offline -q -T 1C -DskipTests -Dlicense.skip=true -Dmaven.javadoc.skip=true \
      -pl application -am package
  local rc=$?
  POM_CHANGED=0
  if [ $rc -ne 0 ]; then
    log "build FAILED (rc=$rc) — keeping previous jar running"
    return $rc
  fi
  publish_jar
}

build_full() {
  log "full build of the monorepo — this takes a while on the first run"
  mvn -T 1C -DskipTests -Dlicense.skip=true -Dmaven.javadoc.skip=true install
  local rc=$?
  if [ $rc -ne 0 ]; then
    log "full build FAILED (rc=$rc)"
    return $rc
  fi
  touch "$STAMP"
  publish_jar
}

case "$MODE" in
  full)
    build_full
    exit $?
    ;;
  once)
    if [ ! -f "$STAMP" ]; then build_full; else build_incremental; fi
    exit $?
    ;;
  watch)
    if [ ! -f "$STAMP" ]; then
      build_full || log "continuing into watch mode despite build failure"
    else
      build_incremental || log "continuing into watch mode despite build failure"
    fi

    log "watching for changes in **/src/main/** and **/pom.xml (interval ${INTERVAL}s)"
    while true; do
      CHANGED=$(inotifywait -q -r -e modify,create,delete,move \
        --format '%w%f' \
        --exclude '(/target/|/node_modules/|/\.git/|/ui-ngx/|\.swp$|~$|/\.idea/)' \
        "$SRC" 2>/dev/null)
      case "$CHANGED" in
        *pom.xml) POM_CHANGED=1 ;;
        */src/main/*) ;;
        *) continue ;;
      esac
      log "change detected: $CHANGED"
      # debounce: let editors/IDE finish writing the whole batch
      sleep "$INTERVAL"
      while inotifywait -q -r -t 2 -e modify,create,delete,move \
        --exclude '(/target/|/node_modules/|/\.git/|/ui-ngx/|/\.idea/)' \
        "$SRC" >/dev/null 2>&1; do :; done
      build_incremental
    done
    ;;
  *)
    log "unknown mode '$MODE' (use: full | once | watch)"
    exit 2
    ;;
esac
