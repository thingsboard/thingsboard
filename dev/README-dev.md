# ThingsBoard — środowisko developerskie (Docker)

Jeden `up.sh` uruchamia infrastrukturę, backend Java 25 z hot-reload i Angular 20 dev
server. Backend da się kompilować **wewnątrz** kontenera — czego oficjalne obrazy
produkcyjne (`thingsboard/tb-node`) nie umożliwiają.

## Instalacja

Skopiuj do repo ThingsBoard:

```text
dev/docker-compose.dev.yml
dev/.env.dev
dev/Dockerfile.tb-node.dev
dev/Dockerfile.ui.dev
dev/tb-node-dev-entrypoint.sh
dev/ui-dev-entrypoint.sh
dev/up.sh
dev/rebuild-backend.sh
dev/bench.sh
ui-ngx/proxy.conf.dev.js
ui-ngx/angular.dev.patch.md      <- diff do angular.json, zastosuj ręcznie
```

```bash
chmod +x dev/*.sh
./dev/up.sh              # pełny hybrid: Postgres + Cassandra + Kafka + Valkey
./dev/up.sh minimal      # tylko Postgres + Kafka + Valkey (najszybszy start)
./dev/up.sh hybrid js    # hybrid + zdalny tb-js-executor
```

Pierwsze uruchomienie jest długie (build modułów Maven + `npm ci` + install schematu).
Kolejne startują z cache w wolumenach nazwanych.

## Porty

| usługa | adres |
| --- | --- |
| Angular dev server | http://localhost:4200 |
| Backend / API | http://localhost:8080 |
| Java remote debug (JDWP) | localhost:5005 |
| Postgres | localhost:5432 |
| Cassandra | localhost:9042 |
| Kafka (external listener) | localhost:9094 |
| Valkey | localhost:6379 |
| MQTT | localhost:1883 |
| CoAP | localhost:5683/udp |

Login demo: `tenant@thingsboard.org` / `tenant`.

## Codzienna praca

```bash
# logi
docker compose --env-file dev/.env.dev -f dev/docker-compose.dev.yml logs -f tb-ui-dev tb-node-dev

# zmiana w Javie — devtools podnosi automatycznie; po zmianie w innym module:
./dev/rebuild-backend.sh dao
./dev/rebuild-backend.sh restart

# pomiar czasów Angulara (tabelka przed/po)
CID=$(docker compose --env-file dev/.env.dev -f dev/docker-compose.dev.yml ps -q tb-ui-dev)
docker cp dev/bench.sh "$CID":/bench.sh
docker exec -it "$CID" bash /bench.sh

# stop
docker compose --env-file dev/.env.dev -f dev/docker-compose.dev.yml --profile hybrid down
```

## Skąd bierze się przyspieszenie

**Frontend**

| zmiana | dlaczego pomaga |
| --- | --- |
| `node_modules` i `.angular/cache` w wolumenach nazwanych | bind-mount tych katalogów na Windows/macOS to główny koszt rebuildu; cache esbuild/Vite przestaje być tracony |
| konfiguracja `fast` (`sourceMap.styles: false`, `budgets: []`, brak optymalizacji) | usuwa najdroższy etap dev builda: mapy źródeł arkuszy i analizę bundle'a |
| TinyMCE (~3000 plików) i `@mdi/svg` (~7500 plików) poza `assets` | kopiowanie tysięcy małych plików przy cold starcie to na Docker Desktop dziesiątki sekund |
| gotowe CSS z `node_modules` jako `<link>` zamiast wpisów w `styles` | 12 arkuszy przestaje przechodzić przez pipeline Sass przy każdej przebudowie |
| `--max_old_space_size` 8048 → 4096 | mniejszy heap = szybszy start V8, krótsze pauzy GC |
| `incremental` + `skipLibCheck` w `tsconfig.app.json` | TypeScript nie typechekuje `.d.ts` z `node_modules` |
| `--hmr` zamiast pełnego live-reload | zmiana w komponencie nie przeładowuje całej aplikacji |
| polling wyłączony domyślnie (`NG_POLL=false`) | na Linux/WSL2 natywne inotify; polling to stały narzut CPU |

**Backend**

| zmiana | dlaczego pomaga |
| --- | --- |
| `maven:3.9-eclipse-temurin-25` + `~/.m2` w wolumenie | kompilacja w kontenerze, zależności ściągane raz |
| `mvn -o` (offline) po pierwszym buildzie | brak odpytywania Maven Central przy każdym starcie |
| `spring-boot:run` + devtools | restart kontekstu w sekundy, bez rebuildu obrazu |
| `-XX:TieredStopAtLevel=1`, `UseSerialGC`, `Xmx2g` | JVM startuje szybciej; C2 i G1 są niepotrzebne w dev |
| AppCDS (`-XX:SharedArchiveFile` + `AutoCreateSharedArchive`) | archiwum klas skraca start JVM przy kolejnych uruchomieniach |
| `TRANSPORT_TYPE=local`, `JS_EVALUATOR=local`, `ZOOKEEPER_ENABLED=false` | jeden proces zamiast 4 nodów + 10 replik js-executora z oficjalnego compose |
| install schematu jako osobny, jednorazowy krok | dev-node nie płaci za install przy każdym starcie |

**Infrastruktura**

| zmiana | dlaczego pomaga |
| --- | --- |
| Kafka w trybie KRaft | brak Zookeepera — jeden kontener i kilkanaście sekund mniej |
| `healthcheck` + `depends_on: service_healthy` | koniec restart-loopów backendu czekającego na bazę; oficjalny compose używa gołego `depends_on` |
| Postgres `fsync=off`, `synchronous_commit=off` | install schematu i migracje wielokrotnie szybsze (**tylko dev**) |
| Cassandra `MAX_HEAP_SIZE=1G`, `num_tokens=1` | bootstrap zamiast ~60 s schodzi do kilkunastu |
| `tb-js-executor` w profilu opcjonalnym, 1 replika | oficjalny compose startuje 10 replik |

## Wyniki pomiarów (wypełnij po `dev/bench.sh`)

| scenariusz | przed | po | cel |
| --- | --- | --- | --- |
| cold start Angulara | 180 s | | 8–20 s |
| warm start (cache) | — | | 2–4 s |
| rebuild po zmianie komponentu | 120 s | | 1–2 s |
| start całego stacku | — | | < 60 s |

Uczciwie: **cold start 3 s jest w tej aplikacji nieosiągalny** — pierwszy build kompiluje
cały graf modułów, a `ui-ngx` to jedna z większych aplikacji Angulara w open source.
Osiągalne i realne jest: **warm start 2–4 s** oraz **rebuild 1–2 s**, czyli dokładnie to,
co odczuwasz podczas pracy. Jeśli po pomiarze cold start nadal przekracza ~25 s, kolejne
kroki wymagają już zmian w kodzie aplikacji:

1. lazy-loading modułów widgetów (`home/components/widget`) — dziś większość jest eager,
2. wydzielenie TinyMCE / Ace / Leaflet do `import()` na żądanie,
3. `NG_BUILD_DEBUG_PERF=1` i cięcie 3 najdroższych faz z raportu.

## Windows / WSL2 — przeczytaj przed narzekaniem na wydajność

To bywa różnica między 120 s a 3 s, niezależnie od wszystkich zmian powyżej:

1. Trzymaj repo **w systemie plików WSL2** (`~/thingsboard`), **nie** w `/mnt/c/...`
   ani na dysku Windows widzianym przez `\\wsl$`. Bind-mount przez granicę
   Windows↔Linux ma ogromny narzut na operacjach na wielu małych plikach.
2. Docker Desktop → Settings → Resources: min. 8 GB RAM i 4 CPU; włącz
   „Use the WSL 2 based engine".
3. Docker Desktop → Settings → General: włącz „Enable VirtioFS".
4. Wyłącz skanowanie repo i wolumenów Dockera w Windows Defender.
5. Ustaw `NG_POLL=false` w `dev/.env.dev`, jeśli kod leży w WSL2 (inotify działa).
   `NG_POLL=true` jest potrzebne tylko przy kodzie na dysku Windows.

## Troubleshooting

**`npm ci` wykonuje się przy każdym starcie** — wolumen `ui-node-modules` został
usunięty albo zmienił się `package-lock.json`. Sprawdź:
`docker volume ls | grep tb-dev`.

**Backend nie widzi zmian w Javie** — devtools restartuje tylko po zmianie plików
`.class` w `target`. Uruchom `./dev/rebuild-backend.sh <modul>`.

**Cassandra `unhealthy` przy starcie** — pierwszy bootstrap trwa; healthcheck ma
`start_period: 30s` i 60 prób. Jeśli pada dalej, zwiększ `MAX_HEAP_SIZE` do `2G`.

**Chcę przeinstalować schemat od zera**

```bash
rm dev/.installed
docker compose --env-file dev/.env.dev -f dev/docker-compose.dev.yml --profile hybrid down -v
./dev/up.sh
```

**Port zajęty** — zmień wartości w `dev/.env.dev`.
