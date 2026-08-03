#!/usr/bin/env bash
# Builds only what the backend needs and caches the runtime classpath.
# The Angular frontend (ui-ngx) is NEVER built here: the yarn/npm executions of
# frontend-maven-plugin are skipped and the `yarn-build` profile is not enabled.
set -euo pipefail

WORKSPACE=${WORKSPACE:-/workspace}
STATE_DIR="$WORKSPACE/.docker-dev"
CP_FILE="$STATE_DIR/classpath.txt"
HASH_FILE="$STATE_DIR/poms.sha"

mkdir -p "$STATE_DIR"

MVN_FLAGS=(
  -B
  -pl application -am
  -DskipTests
  -Dmaven.test.skip=true
  -Dlicense.skip=true
  -Dpkg.skip=true
  -Dskip.installnodenpm=true
  -Dskip.installyarn=true
  -Dskip.installnodeyarn=true
  -Dskip.npm=true
  -Dskip.yarn=true
  -Dgpg.skip=true
  -Dmaven.javadoc.skip=true
  -Dmaven.source.skip=true
)

pom_hash() {
  find "$WORKSPACE" -name pom.xml -not -path "*/node_modules/*" -not -path "*/target/*" \
    -exec sha1sum {} + 2>/dev/null | sort | sha1sum | awk '{print $1}'
}

full_build() {
  echo "==> [build] full maven build (backend modules only, frontend skipped)"
  cd "$WORKSPACE"
  mvn install "${MVN_FLAGS[@]}"
  pom_hash > "$HASH_FILE"
  rm -f "$CP_FILE"
}

quick_compile() {
  echo "==> [build] incremental compile"
  cd "$WORKSPACE"
  mvn -o install "${MVN_FLAGS[@]}" || {
    echo "==> [build] offline compile failed, retrying online"
    mvn install "${MVN_FLAGS[@]}"
  }
}

write_classpath() {
  echo "==> [build] resolving runtime classpath"
  cd "$WORKSPACE"
  local deps="$STATE_DIR/deps.txt"
  mvn -o -B -q -pl application dependency:build-classpath \
      -Dmdep.outputFile="$deps" -Dmdep.includeScope=runtime \
      -Dlicense.skip=true -Dpkg.skip=true \
    || mvn -B -q -pl application dependency:build-classpath \
      -Dmdep.outputFile="$deps" -Dmdep.includeScope=runtime \
      -Dlicense.skip=true -Dpkg.skip=true

  # Local module classes come FIRST so your freshly compiled code always wins
  # over the jar that maven installed into ~/.m2.
  local local_classes
  local_classes=$(find "$WORKSPACE" -maxdepth 4 -type d -path "*/target/classes" \
    -not -path "*/node_modules/*" | sort | tr '\n' ':')

  {
    printf '%s' "$WORKSPACE/application/target/classes:"
    printf '%s' "$local_classes"
    cat "$deps"
  } > "$CP_FILE"
  echo "==> [build] classpath cached at $CP_FILE"
}

MODE=${1:-auto}
case "$MODE" in
  full)
    full_build
    write_classpath
    ;;
  compile)
    quick_compile
    ;;
  auto)
    if [ "${TB_FORCE_BUILD:-false}" = "true" ] \
       || [ ! -f "$CP_FILE" ] \
       || [ ! -d "$WORKSPACE/application/target/classes" ]; then
      full_build
      write_classpath
    elif [ ! -f "$HASH_FILE" ] || [ "$(pom_hash)" != "$(cat "$HASH_FILE")" ]; then
      echo "==> [build] pom.xml changed -> rebuilding dependencies"
      full_build
      write_classpath
    else
      echo "==> [build] up to date (use 'make deps' to force a rebuild)"
    fi
    ;;
  classpath)
    write_classpath
    ;;
  *)
    echo "usage: build.sh [auto|full|compile|classpath]" >&2
    exit 1
    ;;
esac
