## Co już jest w Twoim repo (gałąź UserTenants)

Sprawdziłem repo — katalog `dev/` już zawiera pełny stack: `docker-compose.dev.yml`
(Postgres + Cassandra + Kafka KRaft + Valkey + backend + Angular), `Dockerfile.tb-node.dev`,
`Dockerfile.ui.dev`, oba entrypointy, `up.sh`, `rebuild-backend.sh`, `.env.dev`,
oraz `ui-ngx/proxy.conf.dev.js` i `ui-ngx/angular.dev.patch.md`.

Czego w nim brakuje dokładnie do tego, o co prosisz:

1. **Skrypty są w bashu** (`up.sh`, `rebuild-backend.sh`) — na czystym Windows 10 bez WSL/Git Bash nie odpalisz ich klikiem.
2. **Zmiana w kontrolerze nie kompiluje się sama.** Dziś trzeba ręcznie uruchomić `rebuild-backend.sh`. Spring DevTools restartuje kontekst dopiero, gdy ktoś przebuduje pliki `.class`. Ty chcesz: zapisz plik → Ctrl+F5 → widzisz zmianę.
3. **`angular.dev.patch.md` to diff do zastosowania ręcznie** — jeśli nie jest wklejony do `angular.json`, konfiguracja `fast` nie istnieje i kontener UI wywala się przy starcie.

## Co zrobię

Wygeneruję komplet plików w tym projekcie Lovable, w katalogu `docker-dev/`, gotowych do skopiowania 1:1 do repo ThingsBoard. Nowe i zmienione pliki:

### 1. Auto-kompilacja backendu (sedno sprawy)

Nowy `dev/tb-node-watch.sh` uruchamiany jako drugi proces w kontenerze `tb-node-dev`:

- pilnuje `**/src/main/java` we wszystkich modułach przez `inotifywait` (Linux w kontenerze) z fallbackiem na polling co 2 s, gdy pliki leżą na dysku Windows,
- po zapisie pliku uruchamia **tylko** `mvn -o -q compile -pl <moduł zmieniony> -am -DskipTests -Dpkg.skip=true`,
- świeże `.class` lądują w `target/classes`, który Spring DevTools obserwuje → restart kontekstu w 3–8 s,
- w logach: `[watch] zmiana: XController.java → kompiluję application … gotowe (4.1 s)`.

Efekt: zapisujesz kontroler w IDE na Windows, nic nie klikasz, Ctrl+F5 w przeglądarce pokazuje nową odpowiedź API. Zero Javy i Mavena na Windows — wszystko dzieje się w kontenerze.

`Dockerfile.tb-node.dev` dostanie `inotify-tools`, a entrypoint wystartuje watcher w tle obok `spring-boot:run`.

### 2. Sterowanie z Windows bez basha

- `dev/up.ps1` — odpowiednik `up.sh` w PowerShell: profile `minimal` / `hybrid`, jednorazowy install schematu i danych demo, potem `up -d --build`.
- `dev/dev.cmd` — jednoklikowy skrót: `dev.cmd up`, `dev.cmd logs`, `dev.cmd restart-backend`, `dev.cmd rebuild-ui`, `dev.cmd down`, `dev.cmd reset`.
- `dev/rebuild-backend.ps1` — awaryjny pełny rebuild modułu, gdy watcher nie wystarczy (zmiana w `pom.xml`, nowa zależność, zmiana sygnatury beana).

### 3. Frontend całkowicie z Dockera (koniec z `yarn start:fast`)

- `ui-ngx/angular.json` — dopiszę konfigurację `fast` prosto do pliku, zamiast zostawiać ją jako patch do ręcznego wklejenia. Dodam też fallback w entrypoincie: jeśli konfiguracja `fast` nie istnieje, kontener użyje `development` zamiast się wysypać.
- entrypoint UI przełączę z `npm ci` na **yarn** (repo ma `yarn.lock`, nie `package-lock.json` — dziś każdy start robi pełny `npm install` zamiast czytać z cache).
- domyślnie `NG_POLL=true` w `.env.dev`, bo Twoje repo leży na dysku Windows i inotify przez bind-mount nie działa — bez tego HMR Angulara nie zauważy zmian.
- `node_modules` i `.angular/cache` zostają w wolumenach nazwanych (już tak jest — to jest właśnie to, co daje rebuild 1–2 s).

### 4. Instrukcja

`dev/README-dev.md` przepiszę pod Windows 10: wymagania (tylko Docker Desktop), pierwsze uruchomienie, codzienna pętla pracy, co robić przy zmianie `pom.xml` / `package.json`, tabela portów, troubleshooting (port zajęty, wolny rebuild, Defender, VirtioFS, pamięć w Docker Desktop).

## Szczegóły techniczne

- Backend: obraz `maven:3.9-eclipse-temurin-25`, `mvn spring-boot:run` w module `application`, DevTools + JDWP na 5005 (możesz podpiąć debugger z IntelliJ na Windows bez instalowania JDK — IDE potrzebuje tylko SDK do podpowiedzi).
- Repozytorium `~/.m2`, `target` modułu `application`, archiwum AppCDS i `node_modules` w wolumenach nazwanych — bind-mount tych katalogów na Windows to główna przyczyna wolnych buildów.
- Frontend proxy: `proxy.conf.dev.js` kieruje `/api`, `/ws` na `tb-node-dev:8080`, więc na `localhost:4200` masz komplet.
- Pierwsze `up` jest długie (pełny build Maven + `yarn install` + install schematu). Kolejne starty: kilkadziesiąt sekund.
- Nie zmieniam kodu aplikacji ThingsBoard — tylko warstwę dev, `angular.json` i pliki w `dev/`.

## Czego to nie zrobi

Rebuild w 1 s dla zmiany sygnatury klasy albo nowej zależności w `pom.xml` nie jest osiągalny — wtedy leci pełniejszy `mvn install` modułu (kilkadziesiąt sekund). Zwykła zmiana ciała metody w kontrolerze: 3–8 s do restartu kontekstu.
