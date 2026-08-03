#!/usr/bin/env bash
# Starts the ThingsBoard server and restarts it whenever compiled classes change.
# Compile either from your IDE on the host, or with `make compile`.
set -uo pipefail

WORKSPACE=${WORKSPACE:-/workspace}
STATE_DIR="$WORKSPACE/.docker-dev"
CP_FILE="$STATE_DIR/classpath.txt"
DATA_DIR="$WORKSPACE/application/src/main/data"
STAMP="$STATE_DIR/.watch-stamp"

APP_PID=""

java_opts_array() {
  # shellcheck disable=SC2206
  echo ${TB_JAVA_OPTS:--Xms512m -Xmx2g}
}

start_app() {
  echo "==> [run] starting ThingsBoard (http://localhost:8080)"
  # shellcheck disable=SC2046
  java $(java_opts_array) \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend="${TB_DEBUG_SUSPEND:-n}",address=*:5005 \
    -Dinstall.data_dir="$DATA_DIR" \
    -Dspring.config.name=thingsboard \
    -cp "$(cat "$CP_FILE")" \
    org.thingsboard.server.ThingsboardServerApplication &
  APP_PID=$!
}

stop_app() {
  if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    echo "==> [run] stopping ThingsBoard (pid $APP_PID)"
    kill "$APP_PID" 2>/dev/null
    for _ in $(seq 1 30); do
      kill -0 "$APP_PID" 2>/dev/null || break
      sleep 1
    done
    kill -9 "$APP_PID" 2>/dev/null
    wait "$APP_PID" 2>/dev/null
  fi
  APP_PID=""
}

trap 'stop_app; exit 0' SIGTERM SIGINT

classes_fingerprint() {
  find "$WORKSPACE" -maxdepth 4 -type d -path "*/target/classes" -not -path "*/node_modules/*" \
    -exec find {} -type f \( -name '*.class' -o -name '*.yml' -o -name '*.properties' \) -newer "$STAMP" -print \; \
    2>/dev/null | head -n 1
}

start_app

if [ "${TB_WATCH:-true}" != "true" ]; then
  wait "$APP_PID"
  exit $?
fi

touch "$STAMP"
echo "==> [watch] watching compiled classes (interval ${TB_WATCH_INTERVAL:-2}s)"

while true; do
  sleep "${TB_WATCH_INTERVAL:-2}"

  # app died on its own (compile error at runtime, OOM, ...) -> keep watching
  if [ -n "$APP_PID" ] && ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "==> [run] process exited; waiting for the next code change"
    APP_PID=""
  fi

  if [ -n "$(classes_fingerprint)" ]; then
    echo "==> [watch] change detected -> restarting"
    touch "$STAMP"
    sleep 1   # let the compiler finish writing
    touch "$STAMP"
    stop_app
    start_app
  fi
done
