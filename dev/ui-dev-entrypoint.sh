#!/usr/bin/env bash
# Entrypoint Angular dev servera.
set -euo pipefail

cd /app

# proxy.conf.dev.js dostarczamy z obrazu, zeby dzialalo takze bez kopiowania do repo
if [ ! -f /app/proxy.conf.dev.js ]; then
  cp /opt/tb/proxy.conf.dev.js /app/proxy.conf.dev.js
fi

# node_modules jest w wolumenie nazwanym - instalujemy tylko gdy pusty lub package-lock sie zmienil
LOCK_HASH_FILE="/app/node_modules/.tb-lock-hash"
CURRENT_HASH="$( (sha1sum package-lock.json 2>/dev/null || sha1sum package.json) | awk '{print $1}')"

if [ ! -d /app/node_modules/@angular/cli ] || [ "$(cat "$LOCK_HASH_FILE" 2>/dev/null || true)" != "$CURRENT_HASH" ]; then
  echo "[ui-dev] instaluje zaleznosci (jednorazowo / po zmianie lockfile)..."
  npm ci --no-audit --no-fund || npm install --no-audit --no-fund
  echo "$CURRENT_HASH" > "$LOCK_HASH_FILE"
else
  echo "[ui-dev] node_modules z cache - pomijam npm ci"
fi

POLL_ARGS=()
if [ "${NG_POLL:-false}" = "true" ]; then
  # potrzebne tylko gdy zrodla leza na bind-mouncie Windows/macOS
  POLL_ARGS=(--poll 1500)
  echo "[ui-dev] watch przez polling (1500 ms)"
fi

echo "[ui-dev] start ng serve (configuration=${NG_CONFIGURATION:-fast})"
exec node ./node_modules/@angular/cli/bin/ng.js serve \
  --configuration "${NG_CONFIGURATION:-fast}" \
  --host 0.0.0.0 \
  --port 4200 \
  --proxy-config proxy.conf.dev.js \
  --hmr \
  --live-reload \
  --no-open \
  "${POLL_ARGS[@]}"
