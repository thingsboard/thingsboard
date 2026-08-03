#!/usr/bin/env bash
# Detects the database state and installs / upgrades the ThingsBoard schema.
set -euo pipefail

WORKSPACE=${WORKSPACE:-/workspace}
STATE_DIR="$WORKSPACE/.docker-dev"
CP_FILE="$STATE_DIR/classpath.txt"
DATA_DIR="$WORKSPACE/application/src/main/data"

psql_q() {
  psql -qtAX -c "$1"
}

schema_present() {
  local res
  res=$(psql_q "SELECT to_regclass('public.tb_schema_settings') IS NOT NULL;" 2>/dev/null || echo "f")
  [ "$(echo "$res" | tr -d '[:space:]')" = "t" ]
}

schema_version() {
  psql_q "SELECT schema_version FROM public.tb_schema_settings LIMIT 1;" 2>/dev/null | tr -d '[:space:]'
}

run_java() {
  java -cp "$(cat "$CP_FILE")" \
    -Dinstall.data_dir="$DATA_DIR" \
    "$@"
}

wipe_schema() {
  echo "==> [db] dropping and recreating public schema"
  psql_q "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" >/dev/null
}

install_tb() {
  echo "==> [db] running ThingsBoard install (load_demo=${TB_LOAD_DEMO:-true})"
  run_java org.thingsboard.server.ThingsboardInstallApplication \
    --spring.jpa.hibernate.ddl-auto=none \
    --install.load_demo="${TB_LOAD_DEMO:-true}" \
    --spring.jpa.properties.hibernate.hbm2ddl.auto=none \
    --logging.pattern.console="%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"
  echo "==> [db] install finished"
}

upgrade_tb() {
  local from="$1"
  echo "==> [db] running ThingsBoard upgrade from ${from}"
  run_java org.thingsboard.server.ThingsboardInstallApplication \
    --spring.jpa.hibernate.ddl-auto=none \
    --install.upgrade=true \
    --install.upgrade.from_version="${from}" \
    --logging.pattern.console="%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"
  echo "==> [db] upgrade finished"
}

main() {
  local mode="${1:-auto}"

  # `make db-rebuild` / TB_FORCE_REINSTALL=true -> wipe everything and install from scratch
  if [ "$mode" = "reset" ] || [ "${TB_FORCE_REINSTALL:-false}" = "true" ]; then
    wipe_schema
    install_tb
    echo "$(schema_version)" > "$STATE_DIR/schema_target"
    return
  fi

  if ! schema_present; then
    echo "==> [db] empty database detected"
    install_tb
    echo "$(schema_version)" > "$STATE_DIR/schema_target"
    return
  fi

  local current target
  current=$(schema_version)
  target=$(cat "$STATE_DIR/schema_target" 2>/dev/null || echo "")

  # `make db-migrate` -> always run the upgrade path against the current version
  if [ "$mode" = "migrate" ]; then
    echo "==> [db] forced migration requested (current version: ${current:-unknown})"
    upgrade_tb "${TB_UPGRADE_FROM:-$current}"
    echo "$(schema_version)" > "$STATE_DIR/schema_target"
    return
  fi

  if [ -n "$current" ] && [ -n "$target" ] && [ "$current" != "$target" ]; then
    upgrade_tb "$current"
  else
    echo "==> [db] schema already present (version: ${current:-unknown}) - no migration needed"
  fi

  # remember what the running code expects, so the next start can compare
  echo "$current" > "$STATE_DIR/schema_target"
}

main "$@"

