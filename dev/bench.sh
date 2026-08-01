#!/usr/bin/env bash
# Pomiar bazowy i porownawczy czasow Angulara.
# Uruchamiaj wewnatrz kontenera tb-ui-dev:
#   docker compose --env-file dev/.env.dev -f dev/docker-compose.dev.yml exec tb-ui-dev /bench.sh
# lub lokalnie z katalogu ui-ngx:
#   bash ../dev/bench.sh
set -euo pipefail

cd "${UI_DIR:-/app}"
OUT="${OUT:-/tmp/tb-bench.md}"
CONFIG_A="${CONFIG_A:-development}"
CONFIG_B="${CONFIG_B:-fast}"

run_build() {
  local cfg="$1" label="$2"
  local start end
  start=$(date +%s.%N)
  NG_BUILD_DEBUG_PERF=1 node --max-old-space-size=4096 \
    ./node_modules/@angular/cli/bin/ng.js build --configuration "$cfg" \
    > "/tmp/bench-${label}.log" 2>&1 || true
  end=$(date +%s.%N)
  awk -v s="$start" -v e="$end" 'BEGIN{printf "%.1f", e-s}'
}

echo "# Pomiar Angular dev build" > "$OUT"
echo "" >> "$OUT"
echo "| scenariusz | konfiguracja | czas [s] |" >> "$OUT"
echo "| --- | --- | --- |" >> "$OUT"

echo "==> cold build (${CONFIG_A}), cache wyczyszczony"
rm -rf .angular/cache
T=$(run_build "$CONFIG_A" "cold-a"); echo "| cold (bez cache) | $CONFIG_A | $T |" >> "$OUT"

echo "==> warm build (${CONFIG_A}), cache zachowany"
T=$(run_build "$CONFIG_A" "warm-a"); echo "| warm (z cache) | $CONFIG_A | $T |" >> "$OUT"

if grep -q "\"$CONFIG_B\"" angular.json; then
  echo "==> cold build (${CONFIG_B}), cache wyczyszczony"
  rm -rf .angular/cache
  T=$(run_build "$CONFIG_B" "cold-b"); echo "| cold (bez cache) | $CONFIG_B | $T |" >> "$OUT"

  echo "==> warm build (${CONFIG_B})"
  T=$(run_build "$CONFIG_B" "warm-b"); echo "| warm (z cache) | $CONFIG_B | $T |" >> "$OUT"
else
  echo "(konfiguracja '$CONFIG_B' nie istnieje w angular.json - zastosuj angular.dev.patch.md)" >> "$OUT"
fi

echo "" >> "$OUT"
echo "## Najdrozsze fazy (NG_BUILD_DEBUG_PERF)" >> "$OUT"
echo '```' >> "$OUT"
grep -iE "sass|stylesheet|bundle|component-styles|typescript|assets|copy" /tmp/bench-*.log \
  | sort -u | head -40 >> "$OUT" || true
echo '```' >> "$OUT"

echo ""
echo "Wynik: $OUT"
cat "$OUT"
