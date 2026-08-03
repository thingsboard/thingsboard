#!/usr/bin/env bash
set -euo pipefail

export WORKSPACE=${WORKSPACE:-/workspace}
STATE_DIR="$WORKSPACE/.docker-dev"
mkdir -p "$STATE_DIR"

if [ ! -f "$WORKSPACE/pom.xml" ]; then
  echo "!! /workspace does not contain pom.xml"
  echo "!! Set TB_REPO_PATH in docker/.env to your thingsboard checkout."
  exit 1
fi

echo "==> [wait] postgres ${PGHOST}:${PGPORT}"
for _ in $(seq 1 60); do
  pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" >/dev/null 2>&1 && break
  sleep 1
done
pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" >/dev/null 2>&1 || {
  echo "!! postgres is not reachable"; exit 1;
}

case "${1:-run}" in
  run)
    /opt/tb/build.sh auto
    /opt/tb/install-db.sh auto
    exec /opt/tb/run-watch.sh
    ;;
  build)
    exec /opt/tb/build.sh full
    ;;
  compile)
    exec /opt/tb/build.sh compile
    ;;
  install-db|db)
    # make sure SQL resources / classes are up to date before touching the schema
    /opt/tb/build.sh auto
    exec /opt/tb/install-db.sh "${2:-auto}"
    ;;

  shell)
    exec bash
    ;;
  *)
    exec "$@"
    ;;
esac
