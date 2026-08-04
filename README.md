# ThingsBoard – backend w Dockerze, ui-ngx lokalnie

Idea: w Dockerze trzymasz tylko to, co ciężkie i rzadko się zmienia
(baza danych, serwer ThingsBoard, testowy SMTP). `ui-ngx` odpalasz
lokalnie przez `npm start` – masz wtedy hot-reload (zmiana w kodzie =
przeglądarka odświeża się sama, bez rebuildu obrazu) i możesz bez
przeszkód dodawać nowe paczki npm.

## 1. Wymagania

- Docker Desktop z backendem WSL2 (masz)
- Node.js (LTS – sprawdź plik `.nvmrc` / `package.json` w repo
  `ui-ngx`, żeby dobrać wersję)
- Klon repo: `git clone https://github.com/thingsboard/thingsboard.git`

## 2. Start backendu

W katalogu z `docker-compose.yml`:

```powershell
# jednorazowo – tworzy schemat bazy + (opcjonalnie) dane demo
docker compose run --rm -e INSTALL_TB=true -e LOAD_DEMO=true thingsboard-ce

# start wszystkiego w tle
docker compose up -d

# podgląd logów, aż zobaczysz "ThingsBoard started"
docker compose logs -f thingsboard-ce
```

Jeśli **nie** chcesz danych demo, usuń `-e LOAD_DEMO=true` z pierwszej
komendy (schemat i tak zostanie założony).

Backend API/WS działa teraz na `http://localhost:8080`. Domyślne
konta (jeśli był `LOAD_DEMO=true`):

| Rola      | Email                        | Hasło      |
|-----------|-------------------------------|------------|
| Sysadmin  | sysadmin@thingsboard.org      | sysadmin   |
| Tenant    | tenant@thingsboard.org         | tenant     |
| Customer  | customer@thingsboard.org       | customer   |

## 3. Konfiguracja testowego maila (maildev)

W panelu ThingsBoard: **Settings → Outgoing Mail** (jako sysadmin,
`http://localhost:8080`) ustaw:

- SMTP host: `maildev` (nazwa usługi w sieci Dockera – tb-node
  łączy się z nią z wewnątrz kontenera, nie przez `localhost`)
- SMTP port: `1025`
- Bez TLS, bez uwierzytelniania

Każdy mail wysłany przez ThingsBoard (reset hasła, alarmy, itd.)
wyląduje w panelu podglądu: **http://localhost:1080** – nic nie
leci na zewnątrz, więc możesz testować bezpiecznie.

## 4. Frontend (ui-ngx) – lokalnie, szybko

```powershell
cd thingsboard/ui-ngx
npm install
npm start
```

Otwiera się na `http://localhost:4200` z hot-reloadem i domyślnie
proxuje zapytania API/WS do `http://localhost:8080` (czyli do
kontenera `thingsboard-ce` powyżej) – konfiguracja proxy jest już
w repo (`proxy.conf.json`), nic nie trzeba zmieniać, o ile trzymasz
się domyślnego portu 8080.

Zmiany w kodzie Angulara widzisz od razu w przeglądarce – bez
przebudowy jakiegokolwiek obrazu Dockera.

### Dodawanie nowych paczek

Ponieważ `ui-ngx` działa lokalnie (nie w kontenerze), paczki
dodajesz zwykłym:

```powershell
npm install nazwa-paczki --save
```

i restart `npm start` jeśli trzeba.

## 5. Migracje / upgrade backendu

Migracje schematu bazy dotyczą wyłącznie kontenera `thingsboard-ce`
i wykonują się automatycznie przy zmianie wersji obrazu. Gdy
chcesz podbić wersję `thingsboard/tb-node` w `docker-compose.yml`:

```powershell
docker compose stop thingsboard-ce
docker compose run --rm -e UPGRADE_TB=true -e FROM_VERSION=<stara_wersja> thingsboard-ce
docker compose up -d
```

Wersje podbijaj po kolei (np. 4.1 → 4.2 → 4.3), nie przeskakuj.
Zmian we froncie (ui-ngx) to nie dotyczy – tam po prostu robisz
`git pull` / edytujesz kod i `npm start` łapie zmiany na bieżąco.

## 6. Zatrzymanie / restart

```powershell
docker compose down      # stop, dane w wolumenie zostają
docker compose up -d     # start ponownie
```

## Uwaga o WSL2/Windows

Skoro na natywnym Ubuntu nie masz zawieszeń, a na Windows+WSL2 tak –
najczęstsze przyczyny to zbyt mały limit RAM/CPU dla WSL2 (plik
`%UserProfile%\.wslconfig`) albo mocno napuchnięty plik
`ext4.vhdx` (dysk wirtualny WSL). Warto to sprawdzić przy okazji,
ale powyższy setup i tak odciąża Dockera, bo frontend (najcięższy
w budowaniu) w ogóle nie wchodzi do kontenera.
