#!/usr/bin/env bash
# One-shot ThingsBoard schema installation + demo data.
# Runs only once: guarded by a marker file on the persistent /data volume.
set -uo pipefail

JAR=/build/application.jar
MARKER=/data/.tb-installed

log() { echo "[tb-init] $*"; }

if [ -f "$MARKER" ]; then
  log "already installed ($MARKER present) — skipping"
  exit 0
fi

if [ ! -f "$JAR" ]; then
  log "waiting for $JAR — start the builder first: docker compose up builder"
  while [ ! -f "$JAR" ]; do sleep 5; done
fi

DATA_DIR=""
for d in /src/application/target/data /src/application/src/main/data; do
  if [ -d "$d" ]; then DATA_DIR="$d"; break; fi
done
if [ -z "$DATA_DIR" ]; then
  log "ERROR: could not find the ThingsBoard data dir (application/target/data)."
  log "Run a full build first: docker compose run --rm builder full"
  exit 1
fi

LOAD_DEMO="${LOAD_DEMO:-true}"
log "installing schema (load_demo=$LOAD_DEMO, data_dir=$DATA_DIR)"

# Spring Boot 3 moved PropertiesLauncher; try the new package first.
run_install() {
  local launcher="$1"
  java -cp "$JAR" \
    -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
    -Dspring.jpa.hibernate.ddl-auto=none \
    -Dinstall.data_dir="$DATA_DIR" \
    -Dinstall.load_demo="$LOAD_DEMO" \
    -Dspring.datasource.url="${SPRING_DATASOURCE_URL}" \
    -Dspring.datasource.username="${SPRING_DATASOURCE_USERNAME:-postgres}" \
    -Dspring.datasource.password="${SPRING_DATASOURCE_PASSWORD:-postgres}" \
    "$launcher"
}

if ! run_install org.springframework.boot.loader.launch.PropertiesLauncher; then
  log "retrying with the legacy Spring Boot launcher"
  if ! run_install org.springframework.boot.loader.PropertiesLauncher; then
    log "INSTALL FAILED"
    exit 1
  fi
fi

touch "$MARKER"
log "installation complete."
log "demo logins: sysadmin@thingsboard.org/sysadmin, tenant@thingsboard.org/tenant, customer@thingsboard.org/customer"
