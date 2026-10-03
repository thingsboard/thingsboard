# ThingsBoard – Device Ping Task Deliverable

This document describes how to build/test/run the changes delivered for the task:

- Add REST API: `GET /api/device/ping/{deviceId}`
- Add “Ping Device” button on Device Details page (shows popup result)
- Add unit tests for backend ping logic
- Provide brief module overview (application/dao/transport/ui)

---

## 1) Build & Test Instructions

### Prereqs
- Java 17
- Maven
- Docker Desktop + Docker Compose

### Backend build
From repo root:
```bash
./build.sh
```

### Run unit tests (ping tests only)
```bash
cd application
mvn -Dtest=DefaultDevicePingServiceTest test
```

 ### Running Locally (Docker)

The repo provides scripts and compose overlays under `docker/`.
A typical working stack for this task used:
- Postgres
- Kafka
- Valkey

### Start (example)
From `docker/`:
```bash
./docker-create-log-folders.sh
./docker-install-tb.sh --loadDemo
./docker-start-services.sh
```

Open:
- UI: `http://localhost`
- Login (demo): `tenant@thingsboard.org` / `tenant`

---

## Verifying the Feature (Acceptance Checklist)

### UI check
1. Open `http://localhost` and login.
2. Go to **Entities → Devices**.
3. Open a device and click **Ping Device**.
4. Confirm a dialog opens showing Online/Offline, Last Seen, and the Device Id.

### API check (curl)
```bash
JWT=$(curl -s -X POST http://localhost:80/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"tenant@thingsboard.org","password":"tenant"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')

DEVICE_ID=$(curl -s -H "X-Authorization: Bearer $JWT" \
  'http://localhost:80/api/tenant/devices?pageSize=1&page=0' \
  | python3 -c 'import sys,json; j=json.load(sys.stdin); print(j["data"][0]["id"]["id"])')

curl -s -i -H "X-Authorization: Bearer $JWT" \
  "http://localhost:80/api/device/ping/$DEVICE_ID"
```
Expected: HTTP `200` with JSON including `deviceId`, `reachable`, `lastSeen`.

---

## 2) What Changed (Overview)

### Backend
- Added new endpoint: `GET /api/device/ping/{deviceId}`
  - Implemented in `DeviceController` (Spring MVC controller under `/api`).
  - Computes reachability using existing telemetry:
    - Prefer `lastActivityTime` key
    - Fallback to latest timestamp across all latest telemetry keys
  - Reachability window is configurable (default: **5 minutes**):
    - `device.ping.reachability.window.minutes` (Spring property)
- Refactored ping logic into a dedicated service for testability:
  - `DevicePingService` interface
  - `DefaultDevicePingService` implementation

### Frontend
- “Ping Device” button is present on Device Details page.
- On click, UI calls `GET /api/device/ping/{deviceId}` and shows a dialog with:
  - Online/Offline status
  - Last Seen
  - Device Id
  - Message

### Tests
- Added unit tests for ping logic:
  - `DefaultDevicePingServiceTest` (3 tests: reachable, unreachable, fallback)

---

## 3) Key Challenges Encountered & How They Were Addressed

1. **Wrong endpoint mapping (`/api/api/...`)**
   - Root cause: class-level `/api` combined with a method path that also started with `/api`.
   - Fix: controller method mapping uses `/device/ping/{deviceId}` so final route is `/api/device/ping/{deviceId}`.

2. **UI button existed in source but didn’t show in Docker UI**
   - Root cause: Docker `tb-web-ui` container was serving older bundled UI assets.
   - Fix: built a local web-ui image that replaces bundled assets with the current `ui-ngx` build output.

3. **Apple Silicon + docker build plugin issues / Docker Hub pull failures**
   - Root cause: spotify dockerfile-maven-plugin native/JFFI mismatch on ARM, plus intermittent upstream image pull issues.
   - Fix: used local “install artifacts into existing image” Dockerfile approach for backend and web-ui.

---

## 4) Module Comprehension (Short, per module)

### `application/`
**Purpose**: Main Spring Boot application layer (REST controllers, security, orchestration).

**Typical flow**:
UI/Client → `@RestController` endpoint → service layer (business logic) → DAO/Timeseries services → DTO response.

**Ping path**:
`DeviceController.pingDevice(...)` → `DevicePingService.pingDevice(...)` → Timeseries queries → `DevicePingResponse`.

### `dao/`
**Purpose**: Persistence/data-access layer.

- Contains repositories/services that read/write entities and telemetry.
- In this feature, the ping logic relies on existing telemetry access (Timeseries service) exposed via DAO layer.

### `transport/`
**Purpose**: Device protocol ingress.

- MQTT/HTTP/CoAP/LwM2M/SNMP transports receive device data.
- Transport normalizes/authenticates incoming messages and forwards into queue/core.
- Telemetry written by transports is what the ping endpoint uses to infer “reachability”.

### `ui-ngx/` (UI)
**Purpose**: Angular frontend (Device pages, dialogs, HTTP services).

**Ping UI flow**:
Device details page → click “Ping Device” → `DeviceService.pingDevice(...)` → popup dialog displays response.

---

## ) Local Docker Image Workarounds (Used to Deploy Updated Code)

These are practical deployment helpers used in this environment.

### Backend (tb-node) local image update
If your running Docker stack doesn’t include your latest backend code, rebuild server artifacts (`./build.sh`) and then rebuild a local image that installs the produced package onto the existing base image.

The helper Dockerfile used:
- `msa/tb-node/target/Dockerfile.local`

(Exact command may vary based on your local artifact output.)

### Web UI (tb-web-ui) local image update
To ensure the Docker-served UI matches your local `ui-ngx` sources:

1. Ensure `ui-ngx` has been built (it happens as part of `./build.sh` in many setups).
2. Build a local `thingsboard/tb-web-ui:latest` that swaps in the built `public/` assets.

Helper Dockerfile:
- `msa/web-ui/target/Dockerfile.local`

---

##  Where to Look in the Code

Backend:
- `application/src/main/java/org/thingsboard/server/controller/DeviceController.java`
- `application/src/main/java/org/thingsboard/server/service/device/DevicePingService.java`
- `application/src/main/java/org/thingsboard/server/service/device/DefaultDevicePingService.java`
- `application/src/test/java/org/thingsboard/server/service/device/DefaultDevicePingServiceTest.java`

Frontend:
- `ui-ngx/src/app/modules/home/pages/device/device.component.ts`
- `ui-ngx/src/app/modules/home/pages/device/device.component.html`
- `ui-ngx/src/app/modules/home/pages/device/device-ping-dialog.component.ts`
- `ui-ngx/src/app/modules/home/pages/device/device-ping-dialog.component.html`
