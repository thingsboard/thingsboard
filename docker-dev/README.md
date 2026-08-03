# ThingsBoard — środowisko developerskie w Dockerze (backend Java, bez Angulara)

Docker stawia **tylko backend**: Postgres 18 + ThingsBoard budowany **z tego repo**.
Angular (`ui-ngx`) uruchamiasz lokalnie na Windows/Ubuntu i łączy się z `http://localhost:8080`
(REST) oraz `ws://localhost:8080/api/ws` (WebSocket).

Zmieniasz kod Javy → builder przebudowuje moduł → backend sam się restartuje → widzisz nowe API.

---

## Wymagania

| System | Wymagania |
|---|---|
| Ubuntu | `docker`, `docker compose`, `make`, ~8 GB RAM dla Dockera |
| Windows | Docker Desktop + WSL2, PowerShell (`dev.ps1`) |

> **Windows:** repo trzymaj w systemie plików WSL (np. `\\wsl$\Ubuntu\home\<user>\thingsboard`).
> Na `C:\...` Maven i `inotify` działają wielokrotnie wolniej i watcher może nie widzieć zmian.

---

## Pierwsze uruchomienie

```bash
cd docker-dev

# 1. sekrety
make init            # Windows: .\dev.ps1 init
#   -> edytuj .env i wpisz hasło Mailtrap (MAIL_PASSWORD)

# 2. pełny build monorepo (jednorazowo, realnie 20-40 min)
make build           # Windows: .\dev.ps1 build

# 3. start: postgres -> instalacja schematu + demo dane -> backend
make up              # Windows: .\dev.ps1 up
```

Po starcie:

- REST API — <http://localhost:8080>
- Swagger — <http://localhost:8080/swagger-ui.html>
- WebSocket — `ws://localhost:8080/api/ws`
- Postgres — `localhost:5432`, db `thingsboard`, user `postgres`
- Remote debug (IntelliJ) — `localhost:5005`

Konta demo (`LOAD_DEMO=true`):

| Rola | Login | Hasło |
|---|---|---|
| System admin | `sysadmin@thingsboard.org` | `sysadmin` |
| Tenant admin | `tenant@thingsboard.org` | `tenant` |
| Customer | `customer@thingsboard.org` | `customer` |

---

## Codzienny workflow (hot rebuild)

Terminal A:

```bash
make watch     # Windows: .\dev.ps1 watch
```

Terminal B:

```bash
make logs      # Windows: .\dev.ps1 logs
```

Teraz:

1. edytujesz Javę (nowy kontroler, nowe API, zmiana logiki) albo dopisujesz zależność do `pom.xml`,
2. builder wykrywa zmianę i robi inkrementalny `mvn -pl application -am -DskipTests package` (zwykle 1–3 min),
3. nowy jar ląduje w wolumenie `tb-dev-build` jako `application.jar`,
4. entrypoint backendu widzi nowy timestamp jara, zatrzymuje JVM i startuje nową wersję,
5. `curl http://localhost:8080/api/...` zwraca już nową wersję.

Gdy build się nie uda, **stary jar dalej działa** — w logach buildera zobaczysz błąd kompilacji.

### Nowa paczka w `pom.xml`

Watcher rozpoznaje zmianę `pom.xml` i wyłącza tryb offline Mavena dla tego builda,
więc nowa zależność zostanie pobrana. Cache `~/.m2` jest trwały (wolumen `tb-dev-m2-cache`).

---

## Angular lokalnie

```bash
cd ui-ngx
npm install
npm start        # http://localhost:4200
```

Backend w trybie dev wysyła szerokie nagłówki CORS (`allowed-origin-patterns: *`,
`allow-credentials: true`), więc Angular może uderzać wprost w `http://localhost:8080`
bez proxy. Ustaw w swoim `environment` bazowy URL API na `http://localhost:8080`.

Jeśli przeglądarka blokuje WebSocket albo potrzebujesz same-origin cookies, użyj proxy:
skopiuj `docker-dev/angular/proxy.conf.example.js` do `ui-ngx/proxy.conf.js` i uruchom
`ng serve --proxy-config proxy.conf.js`.

---

## Mail (Mailtrap)

Konfiguracja z `conf/thingsboard.env` odpowiada Twojemu snippetowi:

```text
host = sandbox.smtp.mailtrap.io
port = 2525
ssl  = no            -> MAIL_SSL_ENABLE=false
tls  = yes           -> MAIL_SMTP_STARTTLS_ENABLE=true
user/password        -> MAIL_USERNAME / MAIL_PASSWORD w .env
```

Hasła nie trzymamy w repo — `docker-dev/.env` jest w `.gitignore`.
ThingsBoard trzyma też ustawienia poczty w bazie (System settings → Mail server);
zmienne środowiskowe są wartościami startowymi, w UI możesz je nadpisać i wysłać test.

### Szybka weryfikacja SMTP (skrypty)

```bash
make mail-settings                     # wgrywa ustawienia z .env do bazy TB
make mail-test                         # wysyła testowy mail (POST /api/admin/settings/testMail)
make mail-dev TO=ktos@example.com      # wywołuje własny endpoint /api/dev/mail/test
```

Windows: `.\dev.ps1 mail-settings`, `.\dev.ps1 mail-test`, `.\dev.ps1 mail-dev ktos@example.com`.

Bezpośrednio (bez make):

```bash
./scripts/test-mail.sh            # settings + send
./scripts/test-mail.sh send       # tylko wyślij
```

Skrypt loguje się jako `sysadmin@thingsboard.org`, czyta `MAIL_*` z `.env`, zapisuje je
w `/api/admin/settings` i strzela w `/api/admin/settings/testMail`. Wiadomość pojawia się
w Twoim sandboxie na <https://mailtrap.io>.

### Przykładowe endpointy w Javie

W `samples/` leżą gotowe pliki do wklejenia do repo:

| Plik | Docelowa ścieżka |
|---|---|
| `samples/DevMailTestController.java` | `application/src/main/java/org/thingsboard/server/controller/` |
| `samples/DevMailTestControllerTest.java` | `application/src/test/java/org/thingsboard/server/controller/` |

Po skopiowaniu (przy działającym `make watch`) backend przebuduje się sam i dostajesz:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"sysadmin@thingsboard.org","password":"sysadmin"}' | jq -r .token)

curl -X POST "http://localhost:8080/api/dev/mail/test?to=ktos@example.com" \
  -H "X-Authorization: Bearer $TOKEN"

curl -X POST "http://localhost:8080/api/dev/mail/raw?to=ktos@example.com&subject=Hej&body=Testowa+tresc" \
  -H "X-Authorization: Bearer $TOKEN"
```

Oba endpointy widać też w Swaggerze — to zarazem najprostszy przykład, jak dodać własne API
i zobaczyć zmianę bez restartu ręcznego. Test integracyjny odpalasz przez
`mvn -pl application -am -Dtest=DevMailTestControllerTest test` (normalny build używa `-DskipTests`).

> `/api/dev/**` to endpointy wyłącznie developerskie — usuń plik przed wyjściem na produkcję.


---

## Przydatne komendy

| Cel | Ubuntu | Windows |
|---|---|---|
| status | `make ps` | `.\dev.ps1 ps` |
| logi backendu | `make logs` | `.\dev.ps1 logs` |
| logi buildera | `make logs-builder` | `.\dev.ps1 logs-builder` |
| restart backendu | `make restart` | `.\dev.ps1 restart` |
| psql | `make psql` | `.\dev.ps1 psql` |
| stop | `make down` | `.\dev.ps1 down` |
| świeża baza + demo | `make reset-db` | `.\dev.ps1 reset-db` |
| reset wszystkiego (też cache Mavena) | `make reset-all` | `.\dev.ps1 reset-all` |

Jednorazowy build bez watchera: `docker compose run --rm builder once`.

---

## Co siedzi w środku

```text
postgres      postgres:18, healthcheck, wolumen tb-dev-postgres-data
builder       maven:3.9-eclipse-temurin-17, watch-build.sh -> /build/application.jar
tb-init       jednorazowa instalacja schematu + demo (marker /data/.tb-installed)
thingsboard   eclipse-temurin:17-jdk, entrypoint restartuje JVM po zmianie jara
```

- Kolejki i cache: `in-memory` + `caffeine` — **bez Kafki, Zookeepera i Redisa**, żeby dev startował szybko.
  Gdy będziesz potrzebować Kafki, dorzuć serwis `kafka` do compose i ustaw `TB_QUEUE_TYPE=kafka`
  plus `TB_KAFKA_SERVERS=kafka:9092`.
- `tb-init` uruchamia się przy każdym `up`, ale kończy natychmiast, jeśli marker istnieje —
  backend startuje tylko po jego pomyślnym zakończeniu (`service_completed_successfully`).
- Katalog danych instalacyjnych wykrywany automatycznie:
  `application/target/data` (po `mvn package`), w ostateczności `application/src/main/data`.

## Szybki start bez czekania na pełny build

Jeśli chcesz od razu klikać po API, wystartuj tymczasowo oficjalny obraz:

```bash
docker run --rm -p 8080:8080 --network tb-dev_default \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/thingsboard \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  thingsboard/tb-node:4.3.1.3
```

...a po zakończeniu `make build` przełącz się na własny jar (`make up`).

---

## Uwaga

To konfiguracja **wyłącznie developerska**: otwarty CORS, port debugowania 5005,
hasła w `.env`, brak TLS, jeden węzeł. Nie używaj jej na produkcji.
