# Gateway Support in Device Install Framework — Design Spec

## Goal

Extend the device install wizard to support gateway provisioning: creating a gateway device, configuring one or more connectors via shared attributes, and providing the gateway launch command (docker-compose download URL) in post-install instructions.

## New Step Types

### GATEWAY

Creates a gateway device — a device with `additionalInfo: { gateway: true }`.

- **Template:** device JSON (same format as DEVICE step), must include `"additionalInfo": {"gateway": true}`
- **After creation:** fetches device credentials (same as DEVICE)
- **Find-or-create:** no — always creates new (same as DEVICE)
- **Output variables:**
  - `${gateway.id}` — device UUID
  - `${gateway.name}` — device name
  - `${gateway.token}` — access token
  - `${gateway.dockerComposeUrl}` — `/api/device-connectivity/gateway-launch/${gateway.id}/docker-compose/download`

### GATEWAY_CONNECTOR

Configures a connector on a previously created gateway by saving connector config as shared attributes.

- **Template:** connector config JSON — the object with `name`, `type`, `configurationJson`, `logLevel`, etc.
- **Behavior:**
  1. Fetch current `active_connectors` shared attribute from the gateway (may be empty/missing — default to `[]`)
  2. Append connector name to the array
  3. Save updated `active_connectors` as shared attribute on the gateway
  4. Save `{connectorName}: connectorConfig` as shared attribute on the gateway
- **Target entity:** uses `${gateway.id}` from the preceding GATEWAY step
- **Output variables:** `${gatewayConnector.name}` — the connector name from the template
- **Multiple steps:** each GATEWAY_CONNECTOR step appends to `active_connectors`. Two connectors → two steps → `active_connectors = ["Modbus Connector", "MQTT Connector"]`

## Extension: Optional Attributes on Entity Steps

All entity creation steps (DEVICE, GATEWAY, DEVICE_PROFILE, DASHBOARD, RULE_CHAIN) gain two optional fields in the step definition:

```json
{
  "type": "GATEWAY",
  "name": "${deviceName}",
  "template": "gateway.json",
  "serverAttributes": "server-attributes.json",
  "sharedAttributes": "shared-attributes.json"
}
```

- `serverAttributes` — optional, path to JSON file in ZIP. After entity creation, file is read, variables resolved, and saved as `SERVER_SCOPE` attributes on the created entity.
- `sharedAttributes` — optional, path to JSON file in ZIP. Same, saved as `SHARED_SCOPE`.

Both files contain a flat JSON object of key-value pairs:
```json
{
  "firmwareVersion": "1.2.3",
  "configUrl": "${http.host}:${http.port}/config"
}
```

## Changes to Existing Models

### DeviceInstallStep interface (device-package.models.ts)

Add fields:
```typescript
export interface DeviceInstallStep {
  type: InstallStepType;
  name: string;
  file?: string;
  template?: string;
  serverAttributes?: string;   // NEW
  sharedAttributes?: string;   // NEW
}
```

### InstallStepType enum

Add:
```typescript
GATEWAY = 'GATEWAY',
GATEWAY_CONNECTOR = 'GATEWAY_CONNECTOR'
```

### ENTITY_STEP_TYPES set

Add `GATEWAY` and `GATEWAY_CONNECTOR`.

### stepTypeAliasMap

Add:
```typescript
GATEWAY: 'gateway',
GATEWAY_CONNECTOR: 'gatewayConnector'
```

## Variable Resolution Updates

### New named entity outputs

| Step type | Variables |
|-----------|-----------|
| GATEWAY | `${gateway.id}`, `${gateway.name}`, `${gateway.token}`, `${gateway.dockerComposeUrl}` |
| GATEWAY_CONNECTOR | `${gatewayConnector.name}` |

### EntityStepOutput interface

Add optional `dockerComposeUrl` field:
```typescript
export interface EntityStepOutput {
  id: string;
  name: string;
  token?: string;
  dockerComposeUrl?: string;  // NEW
}
```

## Frontend Implementation (createEntity)

### GATEWAY case

Same as DEVICE:
1. Save device via `deviceService.saveDevice(template, {ignoreErrors: true})`
2. Fetch credentials via `deviceService.getDeviceCredentials(id)`
3. Return output with `dockerComposeUrl` computed from the device ID

```typescript
case InstallStepType.GATEWAY: {
  const result = await firstValueFrom(this.deviceService.saveDevice(template, {ignoreErrors: true}));
  const creds = await firstValueFrom(this.deviceService.getDeviceCredentials(result.id.id, false, {ignoreErrors: true}));
  return {
    id: result.id.id,
    name: result.name,
    token: creds.credentialsId,
    dockerComposeUrl: `/api/device-connectivity/gateway-launch/${result.id.id}/docker-compose/download`
  };
}
```

### GATEWAY_CONNECTOR case

1. Read and resolve the connector template from ZIP
2. Extract `name` from the connector config
3. Fetch current `active_connectors` from gateway's shared attributes (or default to `[]`)
4. Append connector name
5. Save both attributes to gateway via `attributeService.saveEntityAttributes()`

```typescript
case InstallStepType.GATEWAY_CONNECTOR: {
  const gatewayOutput = this.entityOutputs.get('gateway');
  if (!gatewayOutput) throw new Error('GATEWAY step must precede GATEWAY_CONNECTOR');
  const gatewayEntityId = { entityType: 'DEVICE', id: gatewayOutput.id };
  
  // Fetch current active_connectors
  const attrs = await firstValueFrom(this.attributeService.getEntityAttributes(
    gatewayEntityId, AttributeScope.SHARED_SCOPE, ['active_connectors'], {ignoreErrors: true}
  ));
  const activeConnectors: string[] = attrs.find(a => a.key === 'active_connectors')?.value || [];
  
  // Add this connector
  const connectorName = template.name;
  if (!activeConnectors.includes(connectorName)) {
    activeConnectors.push(connectorName);
  }
  
  // Save attributes
  await firstValueFrom(this.attributeService.saveEntityAttributes(
    gatewayEntityId, AttributeScope.SHARED_SCOPE,
    [
      { key: 'active_connectors', value: activeConnectors },
      { key: connectorName, value: template }
    ],
    {ignoreErrors: true}
  ));
  
  return { id: gatewayOutput.id, name: connectorName };
}
```

### Attribute saving after any entity step

After `createEntity()` returns, check if the step has `serverAttributes` or `sharedAttributes`. If so, read the file, resolve variables, and save:

```typescript
if (step.serverAttributes) {
  const attrsJson = JSON.parse(this.resolveVariables(this.zipFiles.get(step.serverAttributes)));
  const attrs = Object.entries(attrsJson).map(([key, value]) => ({ key, value }));
  await firstValueFrom(this.attributeService.saveEntityAttributes(entityId, AttributeScope.SERVER_SCOPE, attrs, {ignoreErrors: true}));
}
if (step.sharedAttributes) {
  const attrsJson = JSON.parse(this.resolveVariables(this.zipFiles.get(step.sharedAttributes)));
  const attrs = Object.entries(attrsJson).map(([key, value]) => ({ key, value }));
  await firstValueFrom(this.attributeService.saveEntityAttributes(entityId, AttributeScope.SHARED_SCOPE, attrs, {ignoreErrors: true}));
}
```

## Translation Keys

Add:
```
"iot-hub.device-install-step-type-GATEWAY": "Gateway",
"iot-hub.device-install-step-type-GATEWAY_CONNECTOR": "Gateway Connector"
```

## Example Gateway Package

```
modbus-sensor.zip/
├── device-info.json
├── prerequisites.md
├── form.json
├── gateway.json
├── modbus-connector.json
├── dashboard.json
└── post-install.md
```

`device-info.json`:
```json
{
  "name": "Modbus Sensor",
  "vendor": "Example",
  "hardwareType": "SENSOR",
  "connectivityTypes": ["GATEWAY_MODBUS"],
  "installSteps": {
    "GATEWAY_MODBUS": [
      {"type": "SHOW_INSTRUCTION", "name": "Prerequisites", "file": "prerequisites.md"},
      {"type": "SHOW_FORM", "name": "Configuration", "file": "form.json"},
      {"type": "GATEWAY", "name": "${deviceName} Gateway", "template": "gateway.json"},
      {"type": "GATEWAY_CONNECTOR", "name": "Modbus Connector", "template": "modbus-connector.json"},
      {"type": "DASHBOARD", "name": "Modbus Monitor", "template": "dashboard.json"},
      {"type": "SHOW_INSTRUCTION", "name": "Launch Gateway", "file": "post-install.md"}
    ]
  }
}
```

`gateway.json`:
```json
{
  "name": "${deviceName} Gateway",
  "type": "Gateway",
  "additionalInfo": {"gateway": true}
}
```

`post-install.md`:
```markdown
## Launch Gateway

1. [Download docker-compose.yml](${gateway.dockerComposeUrl})
2. Place the file in a directory and run:

\`\`\`bash
docker compose up
\`\`\`

The gateway will connect to ThingsBoard at `${mqtt.host}:${mqtt.port}` using access token `${gateway.token}`.
```

## No Backend Changes

All new logic is frontend-only:
- GATEWAY uses existing `saveDevice` API (same as DEVICE)
- GATEWAY_CONNECTOR uses existing `saveEntityAttributes` API
- Attribute saving uses existing `saveEntityAttributes` API
- `dockerComposeUrl` is a constructed URL string, not a new endpoint
