# Install-Method Sync with IoT Hub Marketplace — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sync CE's `InstallMethod` enum + labels with the IoT Hub marketplace's expanded 50-value allow-list, and add a "PE only" gate in the device install dialog so users selecting a PE-only integration get a clear message instead of a silently-broken wizard.

**Architecture:** Frontend-only. CE has no backend install-method allow-list (`DeviceInstalledItemDescriptor.selectedInstallMethod` is a free-form `String`), so all work lives in `ui-ngx`. The `GATEWAY_CONNECTOR` step type already handles connector config generically (line 761 of `device-install-dialog.component.ts`), so the 12 new `GATEWAY_*` install methods need no new install logic — only enum + label entries. The 26 new `INTEGRATION_*` methods are PE-only on CE: today CE silently skips `CONVERTER`/`INTEGRATION` steps (line 553), running an empty-progress wizard — this plan replaces that silent skip with an explicit "PE only — upgrade required" panel shown right after connectivity selection.

**Tech Stack:** Angular 20, TypeScript, Material Design (existing `TbDeviceInstallDialogComponent`).

---

## Source of Truth

The marketplace allow-list lives at:
`/home/ashvayka/git/iot-hub/dao/src/main/java/org/thingsboard/iothub/dao/service/impl/ItemDataServiceImpl.java`, constant `VALID_INSTALL_METHODS`.

CE's enum must be a strict subset of the marketplace's allow-list, including every entry. Diverging causes "marketplace accepts the package, CE wizard rejects it / shows raw constant" failures.

PE/CE applicability — extracted from `/home/ashvayka/git/iot-hub/device-library-contribution.md`:

| Group | Constants | CE-supported install behavior |
|-------|-----------|-------------------------------|
| Direct (5) | `DIRECT_HTTP`, `DIRECT_MQTT`, `DIRECT_COAP`, `DIRECT_LWM2M`, `DIRECT_SNMP` | Yes — runs through `DEVICE` + standard steps |
| Gateway (15) | `GATEWAY_MQTT`, `GATEWAY_MODBUS`, `GATEWAY_OPCUA`, `GATEWAY_BACNET`, `GATEWAY_BLE`, `GATEWAY_CAN`, `GATEWAY_FTP`, `GATEWAY_KNX`, `GATEWAY_OCPP`, `GATEWAY_ODBC`, `GATEWAY_REQUEST`, `GATEWAY_REST`, `GATEWAY_SNMP`, `GATEWAY_SOCKET`, `GATEWAY_XMPP` | Yes — generic `GATEWAY_CONNECTOR` step writes connector config to gateway shared attributes regardless of connector type |
| ChirpStack CE (1) | `CHIRPSTACK` | Yes |
| Integrations (29) | `INTEGRATION_*` (all values listed in step 4 of `VALID_INSTALL_METHODS`) | **No — PE only**. Show "PE only" panel, do not start wizard |

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts` | Extend `InstallMethod` enum to 50 values; extend `installMethodLabels` to 50 labels; add new `peOnlyInstallMethods` set |
| Modify | `ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.ts` | Add `peOnlySelected` getter; route `confirmConnectivity()` to a new "PE only" view-state instead of `startWizard()` when user picks a PE-only method |
| Modify | `ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.html` | Add a new conditional panel that renders when `peOnlySelected` is true: title, body copy, "Learn about ThingsBoard PE" link, Close button |
| Modify | `ui-ngx/src/assets/locale/locale.constant-en_US.json` | Add three translation keys for the PE-only panel: `iot-hub.device-install-pe-only-title`, `iot-hub.device-install-pe-only-message`, `iot-hub.device-install-pe-only-learn-more` |

---

## Task 1: Sync `InstallMethod` enum to all 50 values

**Files:**
- Modify: `ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts`

- [ ] **Step 1: Replace the `InstallMethod` enum block**

Find (lines 17–30):
```typescript
export enum InstallMethod {
  DIRECT_HTTP = 'DIRECT_HTTP',
  DIRECT_MQTT = 'DIRECT_MQTT',
  DIRECT_COAP = 'DIRECT_COAP',
  DIRECT_LWM2M = 'DIRECT_LWM2M',
  DIRECT_SNMP = 'DIRECT_SNMP',
  GATEWAY_MQTT = 'GATEWAY_MQTT',
  GATEWAY_MODBUS = 'GATEWAY_MODBUS',
  GATEWAY_OPCUA = 'GATEWAY_OPCUA',
  CHIRPSTACK = 'CHIRPSTACK',
  INTEGRATION_CHIRPSTACK = 'INTEGRATION_CHIRPSTACK',
  INTEGRATION_TTN = 'INTEGRATION_TTN',
  INTEGRATION_LORIOT = 'INTEGRATION_LORIOT'
}
```

Replace with (preserve grouping comments — they mirror the marketplace validator's grouping and make drift visible at review time):
```typescript
export enum InstallMethod {
  // Direct device-to-platform transports
  DIRECT_HTTP = 'DIRECT_HTTP',
  DIRECT_MQTT = 'DIRECT_MQTT',
  DIRECT_COAP = 'DIRECT_COAP',
  DIRECT_LWM2M = 'DIRECT_LWM2M',
  DIRECT_SNMP = 'DIRECT_SNMP',
  // ThingsBoard IoT Gateway connectors
  GATEWAY_MQTT = 'GATEWAY_MQTT',
  GATEWAY_MODBUS = 'GATEWAY_MODBUS',
  GATEWAY_OPCUA = 'GATEWAY_OPCUA',
  GATEWAY_BACNET = 'GATEWAY_BACNET',
  GATEWAY_BLE = 'GATEWAY_BLE',
  GATEWAY_CAN = 'GATEWAY_CAN',
  GATEWAY_FTP = 'GATEWAY_FTP',
  GATEWAY_KNX = 'GATEWAY_KNX',
  GATEWAY_OCPP = 'GATEWAY_OCPP',
  GATEWAY_ODBC = 'GATEWAY_ODBC',
  GATEWAY_REQUEST = 'GATEWAY_REQUEST',
  GATEWAY_REST = 'GATEWAY_REST',
  GATEWAY_SNMP = 'GATEWAY_SNMP',
  GATEWAY_SOCKET = 'GATEWAY_SOCKET',
  GATEWAY_XMPP = 'GATEWAY_XMPP',
  // ChirpStack (CE-compatible LoRaWAN integration)
  CHIRPSTACK = 'CHIRPSTACK',
  // ThingsBoard PE integrations (CE shows "PE only" gate)
  INTEGRATION_APACHE_PULSAR = 'INTEGRATION_APACHE_PULSAR',
  INTEGRATION_AWS_IOT = 'INTEGRATION_AWS_IOT',
  INTEGRATION_AWS_KINESIS = 'INTEGRATION_AWS_KINESIS',
  INTEGRATION_AWS_SQS = 'INTEGRATION_AWS_SQS',
  INTEGRATION_AZURE_EVENT_HUB = 'INTEGRATION_AZURE_EVENT_HUB',
  INTEGRATION_AZURE_IOT_HUB = 'INTEGRATION_AZURE_IOT_HUB',
  INTEGRATION_AZURE_SERVICE_BUS = 'INTEGRATION_AZURE_SERVICE_BUS',
  INTEGRATION_CHIRPSTACK = 'INTEGRATION_CHIRPSTACK',
  INTEGRATION_COAP = 'INTEGRATION_COAP',
  INTEGRATION_CUSTOM = 'INTEGRATION_CUSTOM',
  INTEGRATION_HTTP = 'INTEGRATION_HTTP',
  INTEGRATION_IOT_CREATORS = 'INTEGRATION_IOT_CREATORS',
  INTEGRATION_KAFKA = 'INTEGRATION_KAFKA',
  INTEGRATION_KPN_THINGS = 'INTEGRATION_KPN_THINGS',
  INTEGRATION_LORIOT = 'INTEGRATION_LORIOT',
  INTEGRATION_MQTT = 'INTEGRATION_MQTT',
  INTEGRATION_OPC_UA = 'INTEGRATION_OPC_UA',
  INTEGRATION_PARTICLE = 'INTEGRATION_PARTICLE',
  INTEGRATION_PUB_SUB = 'INTEGRATION_PUB_SUB',
  INTEGRATION_RABBITMQ = 'INTEGRATION_RABBITMQ',
  INTEGRATION_REMOTE = 'INTEGRATION_REMOTE',
  INTEGRATION_SIGFOX = 'INTEGRATION_SIGFOX',
  INTEGRATION_TCP = 'INTEGRATION_TCP',
  INTEGRATION_THINGPARK = 'INTEGRATION_THINGPARK',
  INTEGRATION_THINGPARK_ENTERPRISE = 'INTEGRATION_THINGPARK_ENTERPRISE',
  INTEGRATION_TTI = 'INTEGRATION_TTI',
  INTEGRATION_TTN = 'INTEGRATION_TTN',
  INTEGRATION_TUYA = 'INTEGRATION_TUYA',
  INTEGRATION_UDP = 'INTEGRATION_UDP'
}
```

- [ ] **Step 2: Verify enum count is 50**

Run:
```bash
grep -c '^  [A-Z][A-Z_]* = ' /home/ashvayka/git/ce/ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts
```
Expected: `50`

- [ ] **Step 3: Verify CE values are a subset of marketplace `VALID_INSTALL_METHODS`**

Run:
```bash
diff <(grep -oP "(?<=^  )[A-Z][A-Z_]+(?= = )" /home/ashvayka/git/ce/ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts | sort -u) \
     <(grep -oP '"[A-Z][A-Z_]+"' /home/ashvayka/git/iot-hub/dao/src/main/java/org/thingsboard/iothub/dao/service/impl/ItemDataServiceImpl.java | sed 's/^"//; s/"$//' | head -50 | sort -u)
```
Expected: no output (sets identical).

- [ ] **Step 4: Commit**

```bash
git add ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts
git commit -m "feat(iot-hub): expand InstallMethod enum to marketplace's 50-value allow-list"
```

---

## Task 2: Sync `installMethodLabels` map

**Files:**
- Modify: `ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts`

- [ ] **Step 1: Replace the `installMethodLabels` map**

Find (lines 32–47, after Task 1 the line numbers shift — locate by the `export const installMethodLabels = new Map<string, string>(` declaration):
```typescript
export const installMethodLabels = new Map<string, string>(
  [
    [InstallMethod.DIRECT_HTTP, 'HTTP'],
    [InstallMethod.DIRECT_MQTT, 'MQTT'],
    [InstallMethod.DIRECT_COAP, 'CoAP'],
    [InstallMethod.DIRECT_LWM2M, 'LwM2M'],
    [InstallMethod.DIRECT_SNMP, 'SNMP'],
    [InstallMethod.GATEWAY_MQTT, 'MQTT Gateway'],
    [InstallMethod.GATEWAY_MODBUS, 'Modbus Gateway'],
    [InstallMethod.GATEWAY_OPCUA, 'OPC-UA Gateway'],
    [InstallMethod.CHIRPSTACK, 'ChirpStack'],
    [InstallMethod.INTEGRATION_CHIRPSTACK, 'ChirpStack (PE)'],
    [InstallMethod.INTEGRATION_TTN, 'The Things Stack'],
    [InstallMethod.INTEGRATION_LORIOT, 'LORIOT']
  ]
);
```

Replace with (labels mirror the brief's table and the "Description" column of the marketplace contributor doc; gateway labels keep the existing "X Gateway" pattern; integration labels keep the brand name without a "(PE)" suffix because the dialog's PE-only panel already conveys that):
```typescript
export const installMethodLabels = new Map<string, string>(
  [
    // Direct
    [InstallMethod.DIRECT_HTTP, 'HTTP'],
    [InstallMethod.DIRECT_MQTT, 'MQTT'],
    [InstallMethod.DIRECT_COAP, 'CoAP'],
    [InstallMethod.DIRECT_LWM2M, 'LwM2M'],
    [InstallMethod.DIRECT_SNMP, 'SNMP'],
    // Gateway connectors
    [InstallMethod.GATEWAY_MQTT, 'MQTT Gateway'],
    [InstallMethod.GATEWAY_MODBUS, 'Modbus Gateway'],
    [InstallMethod.GATEWAY_OPCUA, 'OPC-UA Gateway'],
    [InstallMethod.GATEWAY_BACNET, 'BACnet Gateway'],
    [InstallMethod.GATEWAY_BLE, 'BLE Gateway'],
    [InstallMethod.GATEWAY_CAN, 'CAN Gateway'],
    [InstallMethod.GATEWAY_FTP, 'FTP Gateway'],
    [InstallMethod.GATEWAY_KNX, 'KNX Gateway'],
    [InstallMethod.GATEWAY_OCPP, 'OCPP Gateway'],
    [InstallMethod.GATEWAY_ODBC, 'ODBC Gateway'],
    [InstallMethod.GATEWAY_REQUEST, 'Request Gateway'],
    [InstallMethod.GATEWAY_REST, 'REST Gateway'],
    [InstallMethod.GATEWAY_SNMP, 'SNMP Gateway'],
    [InstallMethod.GATEWAY_SOCKET, 'Socket Gateway'],
    [InstallMethod.GATEWAY_XMPP, 'XMPP Gateway'],
    // ChirpStack
    [InstallMethod.CHIRPSTACK, 'ChirpStack'],
    // PE integrations
    [InstallMethod.INTEGRATION_APACHE_PULSAR, 'Apache Pulsar'],
    [InstallMethod.INTEGRATION_AWS_IOT, 'AWS IoT'],
    [InstallMethod.INTEGRATION_AWS_KINESIS, 'AWS Kinesis'],
    [InstallMethod.INTEGRATION_AWS_SQS, 'AWS SQS'],
    [InstallMethod.INTEGRATION_AZURE_EVENT_HUB, 'Azure Event Hub'],
    [InstallMethod.INTEGRATION_AZURE_IOT_HUB, 'Azure IoT Hub'],
    [InstallMethod.INTEGRATION_AZURE_SERVICE_BUS, 'Azure Service Bus'],
    [InstallMethod.INTEGRATION_CHIRPSTACK, 'ChirpStack (Integration)'],
    [InstallMethod.INTEGRATION_COAP, 'CoAP Integration'],
    [InstallMethod.INTEGRATION_CUSTOM, 'Custom Integration'],
    [InstallMethod.INTEGRATION_HTTP, 'HTTP Integration'],
    [InstallMethod.INTEGRATION_IOT_CREATORS, 'IoT Creators'],
    [InstallMethod.INTEGRATION_KAFKA, 'Apache Kafka'],
    [InstallMethod.INTEGRATION_KPN_THINGS, 'KPN Things'],
    [InstallMethod.INTEGRATION_LORIOT, 'LORIOT'],
    [InstallMethod.INTEGRATION_MQTT, 'MQTT Integration'],
    [InstallMethod.INTEGRATION_OPC_UA, 'OPC-UA Integration'],
    [InstallMethod.INTEGRATION_PARTICLE, 'Particle'],
    [InstallMethod.INTEGRATION_PUB_SUB, 'Google Pub/Sub'],
    [InstallMethod.INTEGRATION_RABBITMQ, 'RabbitMQ'],
    [InstallMethod.INTEGRATION_REMOTE, 'Remote Integration'],
    [InstallMethod.INTEGRATION_SIGFOX, 'Sigfox'],
    [InstallMethod.INTEGRATION_TCP, 'TCP Integration'],
    [InstallMethod.INTEGRATION_THINGPARK, 'ThingPark Wireless'],
    [InstallMethod.INTEGRATION_THINGPARK_ENTERPRISE, 'ThingPark Enterprise'],
    [InstallMethod.INTEGRATION_TTI, 'The Things Industries'],
    [InstallMethod.INTEGRATION_TTN, 'The Things Stack'],
    [InstallMethod.INTEGRATION_TUYA, 'Tuya'],
    [InstallMethod.INTEGRATION_UDP, 'UDP Integration']
  ]
);
```

- [ ] **Step 2: Verify label count matches enum count**

Run:
```bash
node -e "
const src = require('fs').readFileSync('/home/ashvayka/git/ce/ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts', 'utf8');
const enumMatches = (src.match(/^  [A-Z][A-Z_]* = '/gm) || []).length;
const labelMatches = (src.match(/\[InstallMethod\./g) || []).length;
console.log('enum:', enumMatches, 'labels:', labelMatches);
process.exit(enumMatches === 50 && labelMatches === 50 ? 0 : 1);
"
```
Expected: `enum: 50 labels: 50` and exit code 0.

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts
git commit -m "feat(iot-hub): add human-readable labels for all 50 install methods"
```

---

## Task 3: Add `peOnlyInstallMethods` set

**Files:**
- Modify: `ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts`

- [ ] **Step 1: Append the PE-only set after `installMethodLabels`**

After the `installMethodLabels` declaration (and the closing `);`), insert a new exported set. Use the actual enum members so a typo causes a compile error:

```typescript

export const peOnlyInstallMethods: ReadonlySet<string> = new Set<string>([
  InstallMethod.INTEGRATION_APACHE_PULSAR,
  InstallMethod.INTEGRATION_AWS_IOT,
  InstallMethod.INTEGRATION_AWS_KINESIS,
  InstallMethod.INTEGRATION_AWS_SQS,
  InstallMethod.INTEGRATION_AZURE_EVENT_HUB,
  InstallMethod.INTEGRATION_AZURE_IOT_HUB,
  InstallMethod.INTEGRATION_AZURE_SERVICE_BUS,
  InstallMethod.INTEGRATION_CHIRPSTACK,
  InstallMethod.INTEGRATION_COAP,
  InstallMethod.INTEGRATION_CUSTOM,
  InstallMethod.INTEGRATION_HTTP,
  InstallMethod.INTEGRATION_IOT_CREATORS,
  InstallMethod.INTEGRATION_KAFKA,
  InstallMethod.INTEGRATION_KPN_THINGS,
  InstallMethod.INTEGRATION_LORIOT,
  InstallMethod.INTEGRATION_MQTT,
  InstallMethod.INTEGRATION_OPC_UA,
  InstallMethod.INTEGRATION_PARTICLE,
  InstallMethod.INTEGRATION_PUB_SUB,
  InstallMethod.INTEGRATION_RABBITMQ,
  InstallMethod.INTEGRATION_REMOTE,
  InstallMethod.INTEGRATION_SIGFOX,
  InstallMethod.INTEGRATION_TCP,
  InstallMethod.INTEGRATION_THINGPARK,
  InstallMethod.INTEGRATION_THINGPARK_ENTERPRISE,
  InstallMethod.INTEGRATION_TTI,
  InstallMethod.INTEGRATION_TTN,
  InstallMethod.INTEGRATION_TUYA,
  InstallMethod.INTEGRATION_UDP
]);
```

Note: this set has 29 entries (every `INTEGRATION_*`). `CHIRPSTACK` (no `INTEGRATION_` prefix) is NOT in this set — it's the CE-compatible LoRaWAN install method.

- [ ] **Step 2: Verify TypeScript compiles**

Run:
```bash
cd /home/ashvayka/git/ce/ui-ngx && npx tsc --noEmit -p tsconfig.app.json 2>&1 | tail -10
```
Expected: no errors mentioning `device-package.models.ts`.

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts
git commit -m "feat(iot-hub): add peOnlyInstallMethods set covering 29 PE-only integrations"
```

---

## Task 4: Add `peOnlySelected` state to install dialog component

**Files:**
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.ts`

- [ ] **Step 1: Import `peOnlyInstallMethods`**

Locate the existing import block (lines 36–47) that destructures from `@shared/models/iot-hub/device-package.models`:

```typescript
import {
  installMethodLabels as INSTALL_METHOD_LABELS,
  DeviceInstallStep,
  DevicePackageInfo,
  ENTITY_STEP_TYPES,
  EntityStepOutput,
  EntityStepProgress,
  FormFieldDefinition,
  FormFieldType,
  InstallStepType,
  stepTypeAliasMap
} from '@shared/models/iot-hub/device-package.models';
```

Replace the destructure list to add `peOnlyInstallMethods`:

```typescript
import {
  installMethodLabels as INSTALL_METHOD_LABELS,
  peOnlyInstallMethods,
  DeviceInstallStep,
  DevicePackageInfo,
  ENTITY_STEP_TYPES,
  EntityStepOutput,
  EntityStepProgress,
  FormFieldDefinition,
  FormFieldType,
  InstallStepType,
  stepTypeAliasMap
} from '@shared/models/iot-hub/device-package.models';
```

- [ ] **Step 2: Add `peOnlySelected` view-state flag and getter**

Locate the connectivity-state block in the class (around lines 92–96):

```typescript
  // Connectivity
  showConnectivitySelector = false;
  availableInstallMethods: string[] = [];
  selectedInstallMethod: string | null = null;
  installMethodLabels = INSTALL_METHOD_LABELS;
```

Replace with:

```typescript
  // Connectivity
  showConnectivitySelector = false;
  showPeOnlyPanel = false;
  availableInstallMethods: string[] = [];
  selectedInstallMethod: string | null = null;
  installMethodLabels = INSTALL_METHOD_LABELS;

  get isSelectedPeOnly(): boolean {
    return this.selectedInstallMethod !== null && peOnlyInstallMethods.has(this.selectedInstallMethod);
  }
```

- [ ] **Step 3: Route auto-selected single install method to PE-only panel when applicable**

Locate the `ngOnInit` branching block (around lines 172–178):

```typescript
    } else if (this.availableInstallMethods.length === 1) {
      this.selectedInstallMethod = this.availableInstallMethods[0];
      this.showConnectivitySelector = false;
      this.startWizard();
    } else {
      this.showConnectivitySelector = true;
    }
```

Replace with:

```typescript
    } else if (this.availableInstallMethods.length === 1) {
      this.selectedInstallMethod = this.availableInstallMethods[0];
      this.showConnectivitySelector = false;
      if (this.isSelectedPeOnly) {
        this.showPeOnlyPanel = true;
      } else {
        this.startWizard();
      }
    } else {
      this.showConnectivitySelector = true;
    }
```

- [ ] **Step 4: Route `confirmConnectivity()` to PE-only panel when user picks a PE-only method**

Locate `confirmConnectivity()` at line 200:

```typescript
  confirmConnectivity(): void {
    if (!this.selectedInstallMethod) {
      return;
    }
    this.startWizard();
  }
```

Replace with:

```typescript
  confirmConnectivity(): void {
    if (!this.selectedInstallMethod) {
      return;
    }
    if (this.isSelectedPeOnly) {
      this.showPeOnlyPanel = true;
      this.cdr.detectChanges();
      return;
    }
    this.startWizard();
  }
```

The connectivity selector branch is already hidden once `showPeOnlyPanel` flips true — see the template change in Task 5 — so no separate `showConnectivitySelector` flip is needed.

- [ ] **Step 5: Verify build**

Run:
```bash
cd /home/ashvayka/git/ce/ui-ngx && npx tsc --noEmit -p tsconfig.app.json 2>&1 | grep -E 'device-install-dialog|device-package' | head -10
```
Expected: no output (file compiles).

- [ ] **Step 6: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.ts
git commit -m "feat(iot-hub): gate PE-only install methods with dedicated panel state"
```

---

## Task 5: Render the PE-only panel in the install dialog template

**Files:**
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.html`

- [ ] **Step 1: Insert the PE-only panel after the connectivity selector block**

Locate the connectivity selector block (lines 213–238 — starts with `@if (!wizardStarted) {` and contains `<!-- Connectivity selector -->`). The structure is:

```html
  @if (!wizardStarted) {
    <!-- Connectivity selector -->
    <div mat-dialog-content>
      ...
    </div>
    <mat-dialog-actions align="end">
      ...
    </mat-dialog-actions>

  } @else if (reviewMode) {
```

The `@if (!wizardStarted)` branch must be tightened so the connectivity selector only renders when `!showPeOnlyPanel`, and a new branch must render the PE-only panel.

Replace the `@if (!wizardStarted) { ... }` outer block (the entire connectivity-selector branch including the `<!-- Connectivity selector -->` content and the closing `</mat-dialog-actions>`) with:

```html
  @if (!wizardStarted && !showPeOnlyPanel) {
    <!-- Connectivity selector -->
    <div mat-dialog-content>
      <div class="tb-device-install-connectivity flex flex-col gap-3">
        <p>{{ 'iot-hub.device-install-select-connectivity' | translate }}</p>
        <div class="flex flex-wrap gap-2">
          @for (ct of availableInstallMethods; track ct) {
            <button mat-stroked-button
                    class="tb-connectivity-button"
                    [class.selected]="selectedInstallMethod === ct"
                    (click)="selectConnectivity(ct)">
              {{ installMethodLabels.get(ct) || ct }}
            </button>
          }
        </div>
      </div>
    </div>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">{{ 'action.cancel' | translate }}</button>
      <button mat-stroked-button color="primary"
              [disabled]="!selectedInstallMethod"
              (click)="confirmConnectivity()">
        {{ 'action.next' | translate }}
      </button>
    </mat-dialog-actions>

  } @else if (showPeOnlyPanel) {
    <!-- PE-only gate -->
    <div mat-dialog-content>
      <div class="tb-device-install-pe-only flex flex-col items-center gap-3 py-6 text-center">
        <mat-icon class="tb-device-install-pe-only-icon">workspace_premium</mat-icon>
        <h3 class="tb-device-install-pe-only-title">
          {{ 'iot-hub.device-install-pe-only-title' | translate }}
        </h3>
        <p class="tb-device-install-pe-only-message">
          {{ 'iot-hub.device-install-pe-only-message' | translate:{ method: installMethodLabels.get(selectedInstallMethod) || selectedInstallMethod } }}
        </p>
        <a mat-stroked-button color="primary"
           href="https://thingsboard.io/products/thingsboard-pe/"
           target="_blank" rel="noopener">
          {{ 'iot-hub.device-install-pe-only-learn-more' | translate }}
        </a>
      </div>
    </div>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">{{ 'action.close' | translate }}</button>
    </mat-dialog-actions>

  } @else if (reviewMode) {
```

(The `@else if (reviewMode) {` line was already there — just leave it as the start of the next branch.)

- [ ] **Step 2: Verify Angular template compiles**

Run:
```bash
cd /home/ashvayka/git/ce/ui-ngx && npx ng build --configuration=production 2>&1 | tail -8
```
Expected: ends with `Application bundle generation complete.` and no template errors.

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.html
git commit -m "feat(iot-hub): render PE-only panel when user selects a PE-only install method"
```

---

## Task 6: Add translation keys for the PE-only panel

**Files:**
- Modify: `ui-ngx/src/assets/locale/locale.constant-en_US.json`

- [ ] **Step 1: Locate the `iot-hub` section's `device-install-*` keys**

Run:
```bash
grep -n 'device-install-select-connectivity\|device-install-title' /home/ashvayka/git/ce/ui-ngx/src/assets/locale/locale.constant-en_US.json
```
This identifies the lines where the existing `device-install-*` keys live so the new keys can be added alongside them.

- [ ] **Step 2: Add the three new keys**

Add these three key/value pairs immediately after the existing `device-install-select-connectivity` key, preserving the surrounding JSON structure (commas, indentation):

```json
"device-install-pe-only-title": "ThingsBoard PE required",
"device-install-pe-only-message": "The {{method}} install method requires ThingsBoard Professional Edition. Upgrade your installation to use this device package.",
"device-install-pe-only-learn-more": "Learn about ThingsBoard PE",
```

`{{method}}` is the MessageFormat parameter passed from the template (`installMethodLabels.get(selectedInstallMethod)`).

- [ ] **Step 3: Verify JSON is valid**

Run:
```bash
node -e "JSON.parse(require('fs').readFileSync('/home/ashvayka/git/ce/ui-ngx/src/assets/locale/locale.constant-en_US.json','utf8')); console.log('OK');"
```
Expected: `OK`.

- [ ] **Step 4: Commit**

```bash
git add ui-ngx/src/assets/locale/locale.constant-en_US.json
git commit -m "feat(iot-hub): add translation keys for PE-only install panel"
```

---

## Task 7: Manual smoke verification

**Files:** none (verification only)

- [ ] **Step 1: Production build sanity check**

Run:
```bash
cd /home/ashvayka/git/ce/ui-ngx && npx ng build --configuration=production 2>&1 | tail -3
```
Expected: ends with `Application bundle generation complete.`

- [ ] **Step 2: Verify CE enum is a strict superset of the previous 12-value enum**

Run:
```bash
for c in DIRECT_HTTP DIRECT_MQTT DIRECT_COAP DIRECT_LWM2M DIRECT_SNMP \
         GATEWAY_MQTT GATEWAY_MODBUS GATEWAY_OPCUA \
         CHIRPSTACK INTEGRATION_CHIRPSTACK INTEGRATION_TTN INTEGRATION_LORIOT; do
  grep -q "${c} = '${c}'" /home/ashvayka/git/ce/ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts \
    && echo "OK: $c" || echo "MISSING: $c"
done
```
Expected: 12 lines starting with `OK:` and zero `MISSING:` lines.

- [ ] **Step 3: Verify all 50 marketplace constants are present**

Run:
```bash
for c in DIRECT_HTTP DIRECT_MQTT DIRECT_COAP DIRECT_LWM2M DIRECT_SNMP \
         GATEWAY_MQTT GATEWAY_MODBUS GATEWAY_OPCUA \
         GATEWAY_BACNET GATEWAY_BLE GATEWAY_CAN GATEWAY_FTP GATEWAY_KNX \
         GATEWAY_OCPP GATEWAY_ODBC GATEWAY_REQUEST GATEWAY_REST \
         GATEWAY_SNMP GATEWAY_SOCKET GATEWAY_XMPP \
         CHIRPSTACK \
         INTEGRATION_APACHE_PULSAR INTEGRATION_AWS_IOT INTEGRATION_AWS_KINESIS \
         INTEGRATION_AWS_SQS INTEGRATION_AZURE_EVENT_HUB INTEGRATION_AZURE_IOT_HUB \
         INTEGRATION_AZURE_SERVICE_BUS INTEGRATION_CHIRPSTACK INTEGRATION_COAP \
         INTEGRATION_CUSTOM INTEGRATION_HTTP INTEGRATION_IOT_CREATORS \
         INTEGRATION_KAFKA INTEGRATION_KPN_THINGS INTEGRATION_LORIOT \
         INTEGRATION_MQTT INTEGRATION_OPC_UA INTEGRATION_PARTICLE \
         INTEGRATION_PUB_SUB INTEGRATION_RABBITMQ INTEGRATION_REMOTE \
         INTEGRATION_SIGFOX INTEGRATION_TCP INTEGRATION_THINGPARK \
         INTEGRATION_THINGPARK_ENTERPRISE INTEGRATION_TTI INTEGRATION_TTN \
         INTEGRATION_TUYA INTEGRATION_UDP; do
  grep -q "${c} = '${c}'" /home/ashvayka/git/ce/ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts \
    && grep -q "InstallMethod.${c}, '" /home/ashvayka/git/ce/ui-ngx/src/app/shared/models/iot-hub/device-package.models.ts \
    || echo "INCOMPLETE: $c"
done
```
Expected: no output (every constant has both an enum entry and a label).

- [ ] **Step 4: Browser smoke test (manual)**

1. Start dev server: `cd /home/ashvayka/git/ce/ui-ngx && npm start`
2. Log in as a TENANT_ADMIN. Open IoT Hub → Browse.
3. Pick any device package that lists multiple install methods. Open install dialog.
4. **Direct/Gateway/CHIRPSTACK methods:** label renders correctly (no raw constant), Next button starts the wizard normally — no regression.
5. **PE-only method (e.g., `INTEGRATION_AWS_IOT` if the package supports it; otherwise temporarily edit any local test package's `device-info.json` to add a PE-only method):** Next button shows the PE-only panel, the title says "ThingsBoard PE required", the body interpolates the method label (e.g., "The AWS IoT install method requires…"), the "Learn about ThingsBoard PE" link opens `thingsboard.io/products/thingsboard-pe/` in a new tab, the Close button dismisses the dialog.
6. **Auto-selected single PE-only method:** if a package lists only one method and it's PE-only, the dialog should open directly on the PE-only panel (no connectivity selector).

If any check fails, mark this task incomplete and address before merging.

---

## Out of scope

- **No backend allow-list to update.** CE's `DeviceInstalledItemDescriptor.selectedInstallMethod` is a free-form `String` field; the marketplace validator at `ItemDataServiceImpl.VALID_INSTALL_METHODS` is the single source of truth.
- **No new install behavior for the 12 new gateway connectors.** The existing `GATEWAY_CONNECTOR` step (`device-install-dialog.component.ts:761`) writes any connector config object to the gateway's `active_connectors` shared attribute regardless of connector type. Connector-specific schemas (BACnet, BLE, KNX, etc.) are creator concerns, not wizard concerns — the creator's connector JSON config is opaque to the wizard.
- **No connector-specific `${gateway.*}` variable additions.** Existing gateway outputs (`${gateway.id}`, `${gateway.name}`, `${gateway.token}`, `${gateway.dockerComposeUrl}`) are sufficient. Per-connector values like ports or hostnames are already supplied via `SHOW_FORM` fields and resolved through the standard variable mechanism.
- **No CE PE→install conversion.** PE integrations stay PE-only; CE shows the gate.
- **No automated unit test parity.** `ui-ngx` has no spec runner configured (no `*.spec.ts` files outside `node_modules`). The bash verifications in Task 7 act as the parity check.
