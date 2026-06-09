# Transport Variables in Device Install Templates — Design Spec

## Goal

Add transport host/port variables to the device install wizard's template resolution so that post-install instructions and entity templates can reference the platform's configured transport endpoints (e.g., `${mqtt.host}`, `${coap.port}`).

## Variables

All 6 transport protocols are supported:

| Variable | Example value | Source |
|----------|--------------|--------|
| `${http.host}` | `demo.thingsboard.io` | Admin settings `connectivity.http.host` |
| `${http.port}` | `8080` | Admin settings `connectivity.http.port` |
| `${https.host}` | `demo.thingsboard.io` | Admin settings `connectivity.https.host` |
| `${https.port}` | `443` | Admin settings `connectivity.https.port` |
| `${mqtt.host}` | `demo.thingsboard.io` | Admin settings `connectivity.mqtt.host` |
| `${mqtt.port}` | `1883` | Admin settings `connectivity.mqtt.port` |
| `${mqtts.host}` | `demo.thingsboard.io` | Admin settings `connectivity.mqtts.host` |
| `${mqtts.port}` | `8883` | Admin settings `connectivity.mqtts.port` |
| `${coap.host}` | `demo.thingsboard.io` | Admin settings `connectivity.coap.host` |
| `${coap.port}` | `5683` | Admin settings `connectivity.coap.port` |
| `${coaps.host}` | `demo.thingsboard.io` | Admin settings `connectivity.coaps.host` |
| `${coaps.port}` | `5684` | Admin settings `connectivity.coaps.port` |

These variables are available in all template and instruction files, alongside form values and entity outputs.

## Data Source

Frontend fetches from existing admin settings API:
```
GET /api/admin/settings/connectivity
```

Returns `DeviceConnectivitySettings` — a `Record<DeviceConnectivityProtocol, DeviceConnectivityInfo>` where each entry has `{ enabled, host, port }`.

The `AdminService.getAdminSettings<DeviceConnectivitySettings>('connectivity')` method and `DeviceConnectivitySettings` model already exist in the codebase.

## Resolution Priority

The `resolveVariables()` function checks sources in this order:
1. Form field values (`formValues[key]`) — e.g., `${deviceName}`
2. Transport connectivity (`transportVars[key]`) — e.g., `${mqtt.host}`
3. Named entity outputs (`entityOutputs[alias].prop`) — e.g., `${device.id}`

Transport variables use dot notation (`mqtt.host`) which currently falls through to entity output resolution. By adding a transport lookup before entity outputs, we ensure `${mqtt.host}` resolves to the transport config rather than looking for a non-existent entity alias called `mqtt`.

## Changes

**Single file:** `ui-ngx/src/app/modules/home/pages/iot-hub/device-install-dialog/device-install-dialog.component.ts`

1. Inject `AdminService` (from `@core/http/admin.service`)
2. In `ngOnInit`, after ZIP parsing, fetch connectivity settings and flatten to `Record<string, string>`:
   ```
   { 'http.host': '...', 'http.port': '8080', 'mqtt.host': '...', 'mqtt.port': '1883', ... }
   ```
3. In `resolveVariables()`, check the transport map for dot-notation keys before falling through to entity outputs

**No backend changes. No new models. No new endpoints.**

## Example Usage in Templates

Post-install instruction (`post-install.md`):
```markdown
#define THINGSBOARD_SERVER  "${mqtt.host}"
#define THINGSBOARD_PORT    ${mqtt.port}
```

Integration template (`integration.json`):
```json
{"baseUrl": "${http.host}:${http.port}"}
```
