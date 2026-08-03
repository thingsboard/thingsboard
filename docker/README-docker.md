# ThingsBoard – backend w Dockerze, frontend lokalnie

Ten zestaw plików uruchamia **wyłącznie backend** (`application` + zależne moduły Javy) oraz Postgresa.
Angular (`ui-ngx`) **nie jest budowany w Dockerze** – to była najwolniejsza część poprzedniego setupu.
Frontend uruchamiasz u siebie (`yarn start` w `ui-ngx`), a on rozmawia z `http://localhost:8080`.

## 1. Instalacja

Skopiuj katalog `docker/` oraz `Makefile` do katalogu głównego swojego checkoutu
`michal13171/thingsboard` (albo zostaw je gdziekolwiek i ustaw `TB_REPO_PATH`).

```bash
make init          # tworzy docker/.env
# ewentualnie edytuj docker/.env -> TB_REPO_PATH=/sciezka/do/thingsboard
make up            # pierwsze uruchomienie (logi na wierzchu)
```

Pierwszy start pobiera zależności Mavena i buduje moduły backendu – to jednorazowy koszt,
wszystko ląduje w wolumenie `m2`. Kolejne starty to już tylko start JVM.

Gdy w logach pojawi się `Started ThingsboardServerApplication`, backend słucha na
`http://localhost:8080` (REST + WebSocket), dokładnie tak jak przy natywnym uruchomieniu.

Domyślne konto po instalacji z danymi demo:
`sysadmin@thingsboard.org` / `sysadmin`.

## 2. Frontend u siebie

```bash
cd ui-ngx
yarn install
yarn start          # http://localhost:4200, proxy na localhost:8080
```

Nic nie trzeba zmieniać w `proxy.conf.js` – backend stoi pod standardowym portem 8080.

## 3. Codzienna praca – zmiany w kodzie Java

Masz dwa tryby, obydwa działają jednocześnie:

**A. Kompilujesz w swoim IDE (najszybsze, ~15–25 s)**
IntelliJ/VS Code zapisuje klasy do `*/target/classes`, kontener to widzi przez bind mount,
watcher wykrywa zmianę i restartuje JVM. Nie robisz nic więcej.

**B. Kompilacja wewnątrz kontenera**

```bash
make compile        # inkrementalny mvn install (offline), potem auto-restart
```

**C. Debugowanie**
Port `5005` jest wystawiony – podłącz w IDE "Remote JVM Debug" na `localhost:5005`.
Przy podłączonym debuggerze HotSwap ciał metod działa bez restartu.

## 4. Nowe zależności / paczki (zmiana `pom.xml`)

```bash
make deps
```

Pełny build modułów backendu z wykorzystaniem cache Mavena + restart.
Entrypoint dodatkowo sam wykrywa zmianę w plikach `pom.xml` przy starcie kontenera
i wtedy przebudowuje bez pytania.

## 5. Baza danych

- Migracja jest automatyczna przy starcie: pusta baza → pełna instalacja ThingsBoard,
  istniejąca baza ze starszym `schema_version` → upgrade, bez zmian → normalny start.
- Dane trwałe w wolumenie `pgdata`.

Po zmianie schematu wystarczy jedna komenda:

```bash
make db              # rekompilacja backendu + migracje (alias make db-migrate)
```

Pozostałe:

```bash
make db-migrate      # uruchom migracje na istniejącej bazie
make db-rebuild      # pełny wipe + instalacja od zera (gdy migracja nie przejdzie)
make db-shell        # psql
```

`make db` najpierw przebudowuje moduły (żeby zmiany w plikach `.sql` i klasach upgrade'u
trafiły do classpathu), potem zatrzymuje backend, wykonuje migrację i startuje go z powrotem.
Możesz też wymusić start od zera flagą `TB_FORCE_REINSTALL=true` w `docker/.env`,
a wersję źródłową migracji nadpisać przez `TB_UPGRADE_FROM=3.9.0`.

## 6. Wszystkie komendy

```bash
make help
```

| Komenda | Opis |
|---|---|
| `make up` / `make start` | start (foreground / detached) |
| `make down` | stop, dane zostają |
| `make restart` | restart backendu |
| `make logs` | logi backendu |
| `make compile` | inkrementalna kompilacja + hot restart |
| `make deps` | pełny rebuild po zmianie zależności |
| `make rebuild` | przebudowa obrazu Dockera |
| `make db` | rekompilacja + migracje po zmianie schematu |
| `make db-rebuild` | reset bazy i instalacja od zera |
| `make shell` | bash w kontenerze |
| `make clean` | usuwa kontenery **i wolumeny** (baza + cache Mavena) |

## 7. Porty

| Port | Do czego |
|---|---|
| 8080 | REST API + WebSocket (tu celuje `ui-ngx`) |
| 1883 | MQTT |
| 5683/udp | CoAP |
| 7070 | HTTP transport |
| 5005 | debug JVM |
| 5432 | Postgres |

Wszystkie do zmiany w `docker/.env`.

## 8. Dlaczego to jest szybkie

- Zero budowania Angulara: goale `yarn install` / `yarn build` z `frontend-maven-plugin`
  są wyłączone flagami `-Dskip.yarn -Dskip.npm -Dskip.installnodenpm`, profil `yarn-build`
  nie jest włączany.
- `-Dpkg.skip=true` – brak boot-jara, `.deb`, `.rpm` i ZIP-a, które w tym repo są
  budowane przez Gradle w fazie `package`.
- `-pl application -am` – budują się tylko moduły faktycznie potrzebne backendowi
  (bez `msa`, `monitoring`, `rest-client`, `tools`).
- Uruchomienie przez `java -cp` z zapamiętanym classpathem zamiast `mvn spring-boot:run`,
  więc restart to sam start JVM – bez przechodzenia przez Mavena.
- Repozytorium `~/.m2` w nazwanym wolumenie, kompilacja offline (`mvn -o`) z fallbackiem online.
- Kolejka `in-memory` i cache `caffeine` – bez Kafki, Zookeepera i Redisa.

## 9. Windows

Działa na Docker Desktop + WSL2. Dla wydajności bind mountów trzymaj repozytorium
**wewnątrz WSL2** (np. `\\wsl$\Ubuntu\home\...`), nie na dysku `C:` –
mount przez `/mnt/c` potrafi spowolnić kompilację kilkukrotnie.
