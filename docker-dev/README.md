# ThingsBoard — środowisko dev w Dockerze (Windows 10)

Cały stack działa w kontenerach. **Na Windows instalujesz tylko Docker Desktop** —
żadnej Javy, Mavena, Node ani yarna.

- zmieniasz metodę w kontrolerze Springa → kontener sam kompiluje → Ctrl+F5 pokazuje nową odpowiedź,
- zmieniasz komponent Angulara → HMR przeładowuje widok, bez `yarn start:fast`,
- Postgres, Cassandra, Kafka i Valkey wstają z tego samego `docker compose`.

## 1. Instalacja plików w repo

Skopiuj zawartość tego katalogu do repo ThingsBoard, zachowując strukturę:

```text
docker-dev/dev/*                  ->  <repo>/dev/
docker-dev/ui-ngx/proxy.conf.dev.js  ->  <repo>/ui-ngx/proxy.conf.dev.js
```

Pliki nadpisują wcześniejszą wersję katalogu `dev/`. `docker-dev/patches/` to
opcjonalna zmiana w `application/pom.xml` — patrz punkt 6.

## 2. Docker Desktop — ustawienia (zrób to raz)

| miejsce | ustawienie |
| --- | --- |
| Settings → General | włącz „Use the WSL 2 based engine" |
| Settings → Resources | min. **8 GB RAM**, **4 CPU**, 60 GB dysku |
| Settings → General | włącz „Enable VirtioFS" jeśli dostępne |
| Windows Defender | wyklucz katalog repo i `C:\ProgramData\DockerDesktop` ze skanowania |

Bez 8 GB RAM pierwszy build Mavena potrafi paść na braku pamięci.

## 3. Pierwsze uruchomienie

W PowerShell, z katalogu repo:

```powershell
.\dev\dev.cmd up
```

albo bez Cassandry (szybszy start, telemetria w Postgresie):

```powershell
.\dev\dev.cmd up-minimal
```

Pierwszy raz trwa **długo** (pełny build Mavena + `yarn install` + instalacja
schematu i danych demo) — nawet 30–60 minut zależnie od maszyny. Wszystko ląduje
w wolumenach nazwanych, więc kolejne starty to kilkadziesiąt sekund.

Podgląd postępu:

```powershell
.\dev\dev.cmd logs
```

| usługa | adres |
| --- | --- |
| Angular dev server | http://localhost:4200 |
| Backend / API | http://localhost:8080 |
| Java remote debug (JDWP) | localhost:5005 |
| Postgres | localhost:5432 |
| Cassandra | localhost:9042 |
| Kafka (listener zewnętrzny) | localhost:9094 |
| Valkey | localhost:6379 |
| MQTT | localhost:1883 |

Logowanie: `tenant@thingsboard.org` / `tenant`.

## 4. Codzienna praca

### Zmiana w kontrolerze Springa

1. Edytujesz `.java` w IDE na Windows i zapisujesz.
2. Watcher w kontenerze widzi zapis, kompiluje **tylko** zmieniony moduł Mavena.
3. W logach (`.\dev\dev.cmd logs-be`) zobaczysz:

```text
[watch] zmiana: DeviceController.java -> kompiluje modul application
[watch] gotowe (5 s)
```

4. Ctrl+F5 w przeglądarce — nowa odpowiedź API.

Nic nie klikasz, nic nie przebudowujesz ręcznie.

### Zmiana w Angularze

Zapisujesz plik w `ui-ngx/src` — dev server w kontenerze przebudowuje moduł
i podmienia go przez HMR (zwykle 1–3 s). `yarn start:fast` nie jest już potrzebny.

### Kiedy watcher nie wystarczy

Zmiany poza kodem Javy (nowa zależność w `pom.xml`, `application.yml`, zasoby SQL):

```powershell
.\dev\dev.cmd restart-backend            # rebuild modulu application
.\dev\dev.cmd restart-backend dao        # rebuild konkretnego modulu
```

Zmiana `package.json` / `yarn.lock` — kontener UI sam wykryje nowy hash lockfile
i przeinstaluje zależności przy następnym starcie:

```powershell
.\dev\dev.cmd down
.\dev\dev.cmd up
```

### Pozostałe komendy

```powershell
.\dev\dev.cmd ps          # status uslug
.\dev\dev.cmd logs-fe     # logi Angulara
.\dev\dev.cmd shell-be    # konsola w kontenerze backendu
.\dev\dev.cmd down        # stop
.\dev\dev.cmd reset       # stop + skasowanie wszystkich wolumenow (start od zera)
.\dev\dev.cmd rebuild     # start z przebudowa obrazow dev (po zmianie Dockerfile)
```

## 5. Debugowanie z IntelliJ / VS Code

Backend wystawia JDWP na `localhost:5005`. W IntelliJ: *Run → Edit Configurations →
Remote JVM Debug → host `localhost`, port `5005`*. Breakpointy działają, mimo że
JDK jest tylko w kontenerze (IDE potrzebuje lokalnego SDK wyłącznie do podpowiedzi
składni).

## 6. Szybszy reload backendu (opcjonalnie)

Domyślnie po zmianie kodu restartowany jest cały proces aplikacji (30–60 s).
Dodanie `spring-boot-devtools` w profilu `dev` skraca to do **3–8 s** — instrukcja
w `docker-dev/patches/application-pom-devtools.md`. Watcher wykrywa DevTools
automatycznie (`TB_DEV_RELOAD=auto`).

## 7. Skąd bierze się przyspieszenie

**Frontend**

| zmiana | dlaczego pomaga |
| --- | --- |
| `node_modules` i `.angular/cache` w wolumenach nazwanych | bind-mount tych katalogów na Windows to główny koszt rebuildu; cache esbuild przestaje ginąć |
| konfiguracja `fast` (`sourceMap.styles: false`, `budgets: []`, bez optymalizacji) | usuwa najdroższy etap dev builda |
| TinyMCE (~3000 plików) i `@mdi/svg` (~7500 plików) serwowane przez proxy z `node_modules` | kopiowanie tysięcy małych plików przy cold starcie to dziesiątki sekund |
| `--max-old-space-size=4096` zamiast 8048 | mniejszy heap = szybszy start V8, krótsze pauzy GC |
| `--hmr` zamiast pełnego live-reload | zmiana w komponencie nie przeładowuje całej aplikacji |
| yarn zamiast npm | repo ma `yarn.lock`; `npm ci` bez `package-lock.json` i tak robił pełny `npm install` przy każdym starcie |

**Backend**

| zmiana | dlaczego pomaga |
| --- | --- |
| `maven:3.9-eclipse-temurin-25` + `~/.m2` w wolumenie | kompilacja w kontenerze, zależności ściągane raz |
| `mvn -o` (offline) po pierwszym buildzie | brak odpytywania Maven Central przy każdym starcie |
| watcher kompiluje tylko zmieniony moduł | `mvn compile -pl application` zamiast pełnego `install` całego drzewa |
| `-XX:TieredStopAtLevel=1`, `UseSerialGC`, `Xmx2g` | JVM startuje szybciej; C2 i G1 są w dev niepotrzebne |
| AppCDS (`SharedArchiveFile` + `AutoCreateSharedArchive`) | archiwum klas skraca start JVM przy kolejnych uruchomieniach |
| `TRANSPORT_TYPE=local`, `JS_EVALUATOR=local`, `ZOOKEEPER_ENABLED=false` | jeden proces zamiast 4 nodów i 10 replik js-executora |
| install schematu jako osobny, jednorazowy krok | dev-node nie płaci za install przy każdym starcie |

**Infrastruktura**

| zmiana | dlaczego pomaga |
| --- | --- |
| Kafka w trybie KRaft | brak Zookeepera — jeden kontener mniej |
| `healthcheck` + `depends_on: service_healthy` | koniec restart-loopów backendu czekającego na bazę |
| Postgres `fsync=off`, `synchronous_commit=off` | install schematu i migracje wielokrotnie szybsze (**tylko dev**) |
| Cassandra `MAX_HEAP_SIZE=1G`, `num_tokens=1` | bootstrap z ~60 s schodzi do kilkunastu |

## 8. Troubleshooting

**Watcher nie reaguje na zapis pliku** — na dysku Windows inotify nie przechodzi
przez bind-mount. Sprawdź, czy w `dev/.env.dev` jest `TB_DEV_WATCH_POLL=true`
(domyślnie tak). Analogicznie dla Angulara: `NG_POLL=true`.

**Angular nie widzi zmian** — jak wyżej, `NG_POLL=true`. Jeśli repo trzymasz
w systemie plików WSL2 (`\\wsl$\Ubuntu\home\...`), ustaw oba na `false` — polling
to niepotrzebny narzut CPU, a inotify tam działa.

**Wszystko jest wolne** — repo na `C:\` przez bind-mount jest kilkukrotnie wolniejsze
niż w systemie plików WSL2. Największy pojedynczy zysk: przeniesienie repo do
`\\wsl$\Ubuntu\home\<user>\thingsboard` i uruchamianie `dev/up.sh` z WSL.

**Backend restartuje się w kółko** — zobacz `\dev\dev.cmd logs-be`. Najczęściej
błąd kompilacji: watcher pisze `BLAD kompilacji`, a aplikacja pracuje na starym
kodzie do czasu poprawki.

**Port zajęty** — zmień wartości w `dev/.env.dev` (`UI_PORT`, `TB_HTTP_PORT`, …).

**Chcę zacząć od zera**

```powershell
.\dev\dev.cmd reset
.\dev\dev.cmd up
```

**Pierwszy build Mavena pada na braku pamięci** — podnieś RAM w Docker Desktop
do 8–12 GB, ewentualnie zmniejsz `MAVEN_OPTS` w `docker-compose.dev.yml`.

## 9. Czego to nie zrobi

Rebuild w 1 s przy zmianie sygnatury klasy albo nowej zależności w `pom.xml` nie
jest osiągalny — wtedy leci pełniejszy `mvn install` modułu (kilkadziesiąt sekund).
Zwykła zmiana ciała metody w kontrolerze: 3–8 s z DevTools, 30–60 s bez nich.
