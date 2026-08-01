#!/usr/bin/env bash
# Watcher zrodel Javy dla dev-kontenera ThingsBoard.
#
# Pilnuje wszystkich */src/main/java w repo. Po zapisie pliku:
#   1. wykrywa modul Mavena, do ktorego plik nalezy,
#   2. kompiluje TYLKO ten modul (mvn -o compile -pl <modul>),
#   3. sygnalizuje restart aplikacji.
#
# Tryb restartu (TB_DEV_RELOAD):
#   devtools  - swieze .class w target/classes podnosi Spring DevTools (3-8 s)
#   restart   - watcher prosi supervisor w entrypoincie o restart procesu (30-60 s)
#   auto      - devtools jesli jest na classpathie, w innym wypadku restart (domyslne)
set -uo pipefail

SRC_ROOT=/src
RESTART_FLAG=/tmp/tb-restart-requested
RELOAD_MODE="${TB_DEV_RELOAD:-auto}"
DEBOUNCE="${TB_DEV_WATCH_DEBOUNCE:-1}"
POLL_INTERVAL="${TB_DEV_WATCH_POLL_INTERVAL:-2}"

MVN_FLAGS="-o -q -DskipTests -Dlicense.skip=true -Dpkg.skip=true -Dmaven.javadoc.skip=true -Dgpg.skip=true"

log() { echo "[watch] $*"; }

# ------------------------------------------------------------ wykrycie trybu
detect_mode() {
  if [ "$RELOAD_MODE" != "auto" ]; then
    echo "$RELOAD_MODE"
    return
  fi
  if find /root/.m2/repository/org/springframework/boot/spring-boot-devtools \
       -name '*.jar' -print -quit 2>/dev/null | grep -q .; then
    echo devtools
  else
    echo restart
  fi
}

MODE="$(detect_mode)"
log "tryb przeladowania: ${MODE}"
if [ "$MODE" = "restart" ]; then
  log "UWAGA: brak spring-boot-devtools na classpathie -> pelny restart procesu po kazdej zmianie."
  log "       Szybciej bedzie po dodaniu profilu 'dev' do application/pom.xml"
  log "       (patrz docker-dev/patches/application-pom-devtools.md)."
fi

# --------------------------------------------------- modul Mavena dla pliku
module_for() {
  local dir
  dir="$(dirname "$1")"
  while [ "$dir" != "$SRC_ROOT" ] && [ "$dir" != "/" ]; do
    if [ -f "$dir/pom.xml" ]; then
      # sciezka wzgledna wzgledem repo = artefakt -pl dla Mavena
      echo "${dir#"$SRC_ROOT"/}"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  echo "application"
}

# --------------------------------------------------------------- kompilacja
recompile() {
  local file="$1" module started elapsed
  module="$(module_for "$file")"
  started=$(date +%s)
  log "zmiana: $(basename "$file") -> kompiluje modul ${module}"

  if (cd "$SRC_ROOT" && mvn ${MVN_FLAGS} compile -pl "$module" 2>&1 | tail -30); then
    elapsed=$(( $(date +%s) - started ))
    log "gotowe (${elapsed} s)"
    if [ "$MODE" = "devtools" ]; then
      # DevTools obserwuje target/classes - kompilacja sama wyzwala restart kontekstu
      :
    else
      touch "$RESTART_FLAG"
      log "poprosilem o restart aplikacji"
    fi
  else
    log "BLAD kompilacji - aplikacja dziala na starym kodzie, popraw i zapisz ponownie"
  fi
}

# -------------------------------------------------------------------- petla
watch_paths() {
  find "$SRC_ROOT" -type d -path '*/src/main/java' -not -path '*/target/*' 2>/dev/null
}

drain_and_compile() {
  local file="$1"
  # debounce: IDE zapisuje plik kilkoma zdarzeniami, a "Zapisz wszystko" wiele plikow naraz
  sleep "$DEBOUNCE"
  recompile "$file"
}

if command -v inotifywait >/dev/null 2>&1 && [ "${TB_DEV_WATCH_POLL:-false}" != "true" ]; then
  log "obserwuje zrodla przez inotify"
  # shellcheck disable=SC2046
  inotifywait -m -r -e close_write,move,create --format '%w%f' $(watch_paths) 2>/dev/null |
  while read -r changed; do
    case "$changed" in
      *.java) drain_and_compile "$changed" ;;
    esac
  done
else
  log "obserwuje zrodla przez polling co ${POLL_INTERVAL} s (kod na dysku Windows)"
  # znacznik czasu ostatniego przetworzonego zapisu
  STAMP=/tmp/tb-watch-stamp
  touch "$STAMP"
  while true; do
    sleep "$POLL_INTERVAL"
    CHANGED="$(find "$SRC_ROOT" -name '*.java' -path '*/src/main/java/*' \
                 -not -path '*/target/*' -newer "$STAMP" 2>/dev/null | head -1)"
    if [ -n "$CHANGED" ]; then
      touch "$STAMP"
      drain_and_compile "$CHANGED"
      # po dlugiej kompilacji przesuwamy znacznik, zeby nie kompilowac tego samego dwa razy
      touch "$STAMP"
    fi
  done
fi

