# Cel

Jedno polecenie `docker compose up` na Windows lub Ubuntu stawia:
- Postgres 18 z zainstalowanym schematem ThingsBoard + demo danymi (raz, przy pustym wolumenie),
- backend ThingsBoard (`application`) **budowany z Twojego repo** — możesz zmieniać kod Javy, dodawać nowe kontrolery/API, dopisywać zależności do `pom.xml`,
- po rebuildzie kontener sam się restartuje i widzisz zmiany na `localhost:8080/api/...`,
- Mailtrap jako SMTP,
- CORS + WebSocket otwarte, żeby Angular z `localhost:4200` (Windows/Ubuntu/WSL) gadał bezpośrednio z `localhost:8080` bez proxy.

Angular **nie** wchodzi do Dockera.

# Struktura plików do dodania (w repo thingsboard, katalog `docker-dev/`)

```text
docker-dev/
  docker-compose.yml         # postgres + tb-init + tb-backend + builder
  Dockerfile.builder         # maven + jdk, cache ~/.m2, tryb watch
  Dockerfile.backend         # runtime jdk, uruchamia zbudowany jar
  entrypoint-backend.sh      # czeka na jar, uruchamia z JAVA_OPTS + debug 5005
  watch-build.sh             # inotify/mvn -o package przy zmianie w src/**
  conf/
    thingsboard.env          # DB, mailtrap, CORS, logi, cache
    logback.xml              # poziomy logów dev
  init/
    install.sh               # INSTALL_TB=true LOAD_DEMO=true, tylko gdy brak flagi
  Makefile / dev.ps1         # skróty: up, rebuild, logs, psql, reset-db
  README.md                  # instrukcja Windows + Ubuntu
```

# Jak to działa

## 1. Postgres
- `postgres:18`, port `5432:5432` (wystawiony na host, żeby móc podłączyć DBeaver/psql),
- healthcheck `pg_isready`, wolumen `tb-postgres-data`,
- `POSTGRES_DB=thingsboard`, hasło z `.env`.

## 2. Instalacja schematu + demo (kontener `tb-init`)
- uruchamia obraz backendu z `INSTALL_TB=true` i `LOAD_DEMO=true`,
- odpala się tylko raz: `install.sh` sprawdza obecność pliku-znacznika w wolumenie `tb-data` (`/data/.tb-installed`); jeśli jest — kończy natychmiast,
- `depends_on: postgres (service_healthy)`, a backend ma `depends_on: tb-init (service_completed_successfully)`,
- po instalacji dostępny domyślny login demo (`tenant@thingsboard.org` / `tenant`, `sysadmin@thingsboard.org` / `sysadmin`),
- `make reset-db` usuwa wolumeny i wymusza pełną reinstalację.

## 3. Builder (Maven ze źródeł)
- `Dockerfile.builder`: JDK 17 + Maven, wolumen `m2-cache:/root/.m2` żeby drugi build był szybki,
- montuje całe repo read-write, artefakt wypada do współdzielonego wolumenu `tb-build/application.jar`,
- dwa tryby:
  - `docker compose run --rm builder` — jednorazowy `mvn -T 1C -DskipTests install`,
  - `builder` jako serwis w trybie watch (`watch-build.sh`): obserwuje `**/src/main/**` i po zmianie robi inkrementalny `mvn -o -pl application -am -DskipTests package`, potem podmienia jar,
- pierwszy pełny build monorepo to realnie 20–40 min; kolejne (tylko `application` + zmienione moduły) to 1–3 min.

## 4. Backend
- `Dockerfile.backend`: sam JRE/JDK + `entrypoint-backend.sh`,
- uruchamia `java $JAVA_OPTS -jar /build/application.jar`,
- porty: `8080` (HTTP/REST/WS), `7070` (edge), `1883`/`8883` (MQTT), `5683-5688/udp` (CoAP), `5005` (remote debug dla IntelliJ),
- `restart: always` + watcher restartu: gdy timestamp jara się zmieni, proces jest zabijany i entrypoint startuje nową wersję → efekt „zmieniłem Javę, po chwili widzę nowe API”,
- logi `json-file`, max 100 MB × 10.

## 5. Konfiguracja przez zmienne środowiskowe (`conf/thingsboard.env`)
- DB: `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`,
- Mail (Mailtrap, wartości z Twojego snippetu → zmienne TB):
  - `MAIL_SMTP_HOST=sandbox.smtp.mailtrap.io`, `MAIL_SMTP_PORT=2525`,
  - `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS_ENABLE=true`, `MAIL_SSL_ENABLE=false`,
  - `MAIL_USERNAME`/`MAIL_PASSWORD` czytane z `.env` (nie commitujemy hasła),
  - `MAIL_FROM=no-reply@localhost`,
- CORS dev: `HTTP_CORS_ALLOWED_ORIGINS=*` (lub lista `http://localhost:4200,http://127.0.0.1:4200`) + dozwolone metody/nagłówki i `Authorization`; WS na `ws://localhost:8080/api/ws` bez dodatkowego proxy,
- Swagger/OpenAPI włączony na `localhost:8080/swagger-ui.html`, żeby od razu widzieć nowe endpointy,
- cache/queue w trybie in-memory (bez Kafki/Redisa) — minimalna konfiguracja dev.

## 6. Podpięcie Angulara (lokalnie, poza Dockerem)
- w `ui-ngx` uruchamiasz `npm start` na `localhost:4200`,
- `environment.dev.ts` / zmienna proxy wskazuje `http://localhost:8080`,
- README opisze oba warianty: bez proxy (dzięki otwartemu CORS) i z `proxy.conf.js` jako fallback, gdyby przeglądarka blokowała WS.

# Twój codzienny workflow

```text
docker compose up -d              # postgres + install demo + backend
docker compose up builder         # tryb watch (osobny terminal)
# edytujesz Javę / dodajesz dependency do pom.xml
# builder przebudowuje -> backend restartuje
curl localhost:8080/api/...       # widzisz nową wersję
```

- nowa paczka w `pom.xml` → watcher wykrywa zmianę pom i robi `mvn -pl application -am` (przy nowej zależności zdejmuje tryb offline, żeby ją dociągnąć),
- `make logs` / `docker compose logs -f thingsboard`,
- `make reset-db` gdy chcesz świeże demo dane.

# Szczegóły techniczne / założenia

- Wersje: JDK 17, Maven 3.9, Postgres 18, ThingsBoard z Twojego brancha `master`.
- Kolejki i cache: in-memory (`TB_QUEUE_TYPE=in-memory`, caffeine) — świadomie bez Kafki/Redisa dla szybkości devu; README wskaże, jak dołożyć Kafkę, gdy będzie potrzebna.
- Windows: potrzebny Docker Desktop z WSL2; README ostrzeże, że repo powinno leżeć w systemie plików WSL (`\\wsl$`), inaczej Maven i inotify są bardzo wolne. Dla Windows dodaję `dev.ps1` jako odpowiednik `Makefile`.
- Sekrety (hasło Mailtrap, hasło DB) trafiają do `docker-dev/.env` na podstawie `.env.example`; `.env` dopisany do `.gitignore`.
- Pełny pierwszy build jest długi — README poda też opcję „szybki start”: wystartować backend z oficjalnego obrazu i przełączyć na własny jar po zakończeniu builda.

# Czego ten plan nie robi

- nie buduje ani nie serwuje Angulara w Dockerze,
- nie stawia klastra (jeden węzeł tb, bez Kafki/Zookeepera/Redisa),
- nie jest konfiguracją produkcyjną (otwarty CORS, debug port, hasła w `.env` — tylko dev).
