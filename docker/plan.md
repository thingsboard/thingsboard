## Cel

Uruchomić w Dockerze **wyłącznie backend** ThingsBoard (fork `michal13171/thingsboard`), tak aby:
- Twój lokalnie uruchomiony `ui-ngx` (`npm start`, port 4200) łączył się z nim przez `localhost:8080`,
- zmiany w kodzie Java z Twojego edytora były podchwytywane bez pełnego przebudowywania obrazu,
- baza migrowała się automatycznie przy starcie,
- dodanie nowych zależności/paczek dało się obsłużyć jedną komendą rebuildu.

Frontend nie jest budowany w Dockerze w ogóle — to eliminuje najwolniejszą część wczorajszego setupu.

## Co powstanie

```text
docker/
  docker-compose.yml          # postgres + kafka(opcj.) + tb-backend
  backend.Dockerfile          # obraz dev: JDK + Maven + cache zależności
  .env.example                # porty, hasła, profil bazy
  entrypoint.sh               # migracja/instalacja + start Spring Boot
  m2-cache/                   # wolumen repozytorium Maven (nie do gita)
Makefile                      # up / down / rebuild / deps / db-reset / logs
README-docker.md              # instrukcja krok po kroku (Windows + Ubuntu)
```

## Architektura uruchomienia

```text
[ ui-ngx lokalnie :4200 ]  --proxy-->  [ localhost:8080 ]
                                             |
                                     ┌───────────────┐
                                     │ tb-backend     │  ← bind mount kodu z hosta
                                     │ JVM + devtools │
                                     └───────┬────────┘
                                             │
                                     ┌───────────────┐
                                     │ postgres:16    │  ← wolumen danych
                                     └───────────────┘
```

- Backend nasłuchuje na `8080` (REST + WebSocket) — dokładnie tak, jak przy natywnym uruchomieniu, więc `ui-ngx` działa bez zmian w konfiguracji.
- Dodatkowo wystawione: `1883` (MQTT), `5683/udp` (CoAP), `7070` (transport), `5005` (debug JVM do podpięcia z IntelliJ/VS Code).

## Tryby pracy

**1. Hot reload (domyślny, tryb codzienny)**
- Katalog źródeł montowany jako wolumen; skompilowane klasy z Twojego IDE (`target/classes`) są widoczne w kontenerze.
- Spring Boot DevTools restartuje kontekst po zmianie klas — czas ~5-15 s zamiast pełnego builda.
- Repozytorium Maven trzymane w nazwanym wolumenie, więc nie pobiera się od nowa przy każdym starcie.

**2. Rebuild na żądanie (zmiana zależności / nowe paczki)**
- `make deps` — przebudowa modułów Maven wewnątrz kontenera z wykorzystaniem cache i restart backendu.
- `make rebuild` — pełna przebudowa obrazu, gdy zmieni się `Dockerfile` lub wersja JDK.

## Baza danych

- Postgres w osobnym kontenerze z trwałym wolumenem.
- Entrypoint przy każdym starcie wykrywa stan schematu: puste DB → pełna instalacja ThingsBoard (`install.sh`), istniejące DB ze starszą wersją → upgrade. Bez zmian → normalny start.
- Flaga w `.env` do wymuszenia czystej instalacji, plus `make db-reset` gdyby trzeba było zacząć od zera.

## Optymalizacja czasu startu (odpowiedź na 180 s z wczoraj)

- Zero budowania Angulara w obrazie.
- Wielowarstwowy build: warstwa zależności Maven cache'owana osobno od kodu.
- `mvn -o` (offline) i pominięcie testów oraz license-checka w trybie dev.
- Wolumen `m2-cache` współdzielony między rebuildami.
- Pierwszy start: pobranie zależności (jednorazowo). Kolejne starty: kilkanaście sekund. Zmiana w kodzie: restart kontekstu, nie kontenera.

## Szczegóły techniczne

- Baza obrazu: `eclipse-temurin` JDK w wersji zgodnej z `pom.xml` forka (zweryfikuję po sklonowaniu repo).
- Kafka/Redis dołączam tylko jeśli fork ich wymaga w domyślnym profilu; w przeciwnym razie kolejka in-memory dla minimalnego zużycia zasobów.
- Konfiguracja przez zmienne środowiskowe ThingsBoard (`SPRING_DATASOURCE_URL`, `DATABASE_TS_TYPE`, `INSTALL_TB` itd.) — bez modyfikowania plików `.yml` w repo.
- CORS/proxy: `ui-ngx` używa własnego `proxy.conf.js` kierującego na `localhost:8080`, więc po stronie backendu nic nie trzeba zmieniać.
- Pliki wspierają Windows (Docker Desktop/WSL2) i Ubuntu; w README uwaga o wydajności bind mountów na Windows i zaleceniu trzymania repo w WSL2.

## Do potwierdzenia po sklonowaniu repo

Sprawdzę w Twoim forku wersję JDK, listę modułów, wymagane usługi (Kafka/Redis) i skrypty instalacyjne — jeśli coś odbiega od upstreamu ThingsBoard, dostosuję compose i zgłoszę to w podsumowaniu.
