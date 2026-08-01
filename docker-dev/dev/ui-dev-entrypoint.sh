#!/usr/bin/env bash
# Entrypoint Angular dev servera (ui-ngx) w kontenerze.
set -uo pipefail

cd /app

log() { echo "[ui-dev] $*"; }

# proxy dostarczamy z obrazu, zeby dzialalo takze bez kopiowania pliku do repo
if [ ! -f /app/proxy.conf.dev.js ]; then
  cp /opt/tb/proxy.conf.dev.js /app/proxy.conf.dev.js
fi

# ------------------------------------------------- zaleznosci (wolumen nazwany)
LOCK_HASH_FILE="/app/node_modules/.tb-lock-hash"
CURRENT_HASH="$( (sha1sum yarn.lock 2>/dev/null || sha1sum package.json) | awk '{print $1}')"

if [ ! -d /app/node_modules/@angular/cli ] || [ "$(cat "$LOCK_HASH_FILE" 2>/dev/null || true)" != "$CURRENT_HASH" ]; then
  log "instaluje zaleznosci przez yarn (jednorazowo / po zmianie yarn.lock)..."
  if [ -f yarn.lock ]; then
    yarn install --frozen-lockfile --network-timeout 600000 || yarn install --network-timeout 600000
  else
    npm install --no-audit --no-fund
  fi
  echo "$CURRENT_HASH" > "$LOCK_HASH_FILE"
else
  log "node_modules z cache - pomijam instalacje"
fi

# ------------------------------------------------------------ konfiguracja ng
CONFIG="${NG_CONFIGURATION:-fast}"
if ! node -e "
  const c = require('/app/angular.json');
  const s = Object.values(c.projects)[0].architect.serve.configurations || {};
  process.exit(s['${CONFIG}'] ? 0 : 1);
" 2>/dev/null; then
  log "konfiguracja '${CONFIG}' nie istnieje w angular.json - uzywam 'development'"
  CONFIG=development
fi

POLL_ARGS=()
if [ "${NG_POLL:-true}" = "true" ]; then
  # wymagane, gdy repo lezy na dysku Windows - inotify nie przechodzi przez bind-mount
  POLL_ARGS=(--poll "${NG_POLL_INTERVAL:-1500}")
  log "watch przez polling (${NG_POLL_INTERVAL:-1500} ms)"
fi

log "start ng serve (configuration=${CONFIG}) -> http://localhost:4200"
exec yarn start:fast
