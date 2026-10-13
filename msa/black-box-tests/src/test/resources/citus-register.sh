#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

set -euo pipefail

COORDINATOR_HOST=postgres
WORKERS=(citus-worker1 citus-worker2)
PORT=5432
DB=thingsboard
DB_USER=postgres
export PGPASSWORD=postgres

wait_for() {
  local host="$1"
  local max_attempts=60
  local attempt=1
  echo "Waiting for ${host}:${PORT} to accept connections..."
  until pg_isready -h "$host" -p "$PORT" -U "$DB_USER" -d "$DB" >/dev/null 2>&1; do
    if [ "$attempt" -ge "$max_attempts" ]; then
      echo "Timed out waiting for ${host}:${PORT} after ${max_attempts} attempts." >&2
      exit 1
    fi
    attempt=$((attempt + 1))
    sleep 2
  done
  echo "${host} is ready."
}

wait_for "$COORDINATOR_HOST"
for w in "${WORKERS[@]}"; do
  wait_for "$w"
done

# Generate the citus_add_node calls from ${WORKERS[@]} so the registered nodes stay in sync
# with the wait_for list above — adding a worker means editing the array in one place only.
ADD_NODES=""
for w in "${WORKERS[@]}"; do
  ADD_NODES+="SELECT citus_add_node('${w}', ${PORT});"$'\n'
done

psql -h "$COORDINATOR_HOST" -p "$PORT" -U "$DB_USER" -d "$DB" -v ON_ERROR_STOP=1 <<SQL
CREATE EXTENSION IF NOT EXISTS citus;
SELECT citus_set_coordinator_host('${COORDINATOR_HOST}', ${PORT});
${ADD_NODES}SELECT nodename, nodeport, noderole FROM pg_dist_node ORDER BY groupid;
SQL

echo "Citus cluster registered (coordinator + 2 workers)."
