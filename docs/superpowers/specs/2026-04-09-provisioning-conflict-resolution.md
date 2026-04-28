# Provisioning Conflict Resolution — Design Spec

## Goal

When an entity with the same name already exists during device package provisioning, show a conflict resolution UI instead of an error. The user chooses how to proceed (use existing, overwrite, or create copy) and provisioning continues.

## Conflict Detection

Before creating each entity, pre-check by name. If an entity with the same name exists, show the conflict UI instead of attempting creation.

| Entity type | Pre-check | Conflict options |
|-------------|-----------|-----------------|
| DEVICE_PROFILE | `findDeviceProfileByName` | Use existing / Overwrite |
| RULE_CHAIN | `findRuleChainByName` | Use existing / Overwrite |
| DEVICE | `findDeviceByName` (new) | Use existing / Overwrite |
| GATEWAY | `findDeviceByName` (new, same API) | Use existing / Overwrite |
| DASHBOARD | `findDashboardByName` (new) | Overwrite / Create copy |
| GATEWAY_CONNECTOR | no pre-check | — |

## New Status: `conflict`

```typescript
export type EntityStepStatus = 'pending' | 'running' | 'success' | 'error' | 'conflict';
```

`EntityStepProgress` gains:
```typescript
existingEntity?: EntityStepOutput;  // the found entity, for resolution
conflictType?: 'use-or-overwrite' | 'overwrite-or-copy';  // which buttons to show
```

## Resolution Actions

**Use existing**: take the existing entity's ID/name/url, store as output, continue to next step. No API call.

**Overwrite**: fetch existing entity by ID, merge template data (keeping the ID), save. For rule chains, also save metadata. Store result as output.

**Create copy**: create new entity with the template as-is (TB allows duplicate dashboard names). Store result as output.

## UI

Conflict row styling:
- Amber/yellow background (not red — it's a decision, not an error)
- Warning icon (amber `⚠`)
- Description: "Device profile with this name already exists"
- Two buttons on the right

For `use-or-overwrite`:
```
⚠  Device Profile — Modbus TH Sensor
   Entity with this name already exists
                                    [Use existing]  [Overwrite]
```

For `overwrite-or-copy`:
```
⚠  Dashboard — Modbus TH Monitor
   Dashboard with this name already exists
                                    [Overwrite]  [Create copy]
```

Button styling:
- Use existing / Create copy: outlined primary (safe option)
- Overwrite: outlined warning/amber (destructive)

## Provisioning Step Flow

1. For each entity step:
   a. Set status = 'running'
   b. Pre-check: search for existing entity by name
   c. If found: set status = 'conflict', store existing entity, pause (return from loop)
   d. If not found: create entity → success → continue
   e. If creation fails: set status = 'error' → pause (existing behavior)
2. When user clicks a conflict resolution button:
   a. Execute resolution (use existing / overwrite / create copy)
   b. Set status = 'success'
   c. Resume `runEntitySteps` from the next step

## Read-Only Review Dialog

No changes needed — provisioning step shows all entities as success with "Done" label. No conflict UI in review mode.

## Changes

### Models (`device-package.models.ts`)
- Add `'conflict'` to `EntityStepStatus`
- Add `existingEntity?: EntityStepOutput` and `conflictType?: string` to `EntityStepProgress`

### Dialog Component (`device-install-dialog.component.ts`)
- Add `findDeviceByName` and `findDashboardByName` methods
- Refactor `createEntity` to pre-check before creating
- Add `resolveConflict(ep, resolution)` method
- Resume loop after conflict resolution

### Dialog Template (`device-install-dialog.component.html`)
- Add conflict row rendering with amber styling and resolution buttons

### Dialog Styles (`device-install-dialog.component.scss`)
- Add `.tb-progress-conflict` amber styles

### Translations
- Add conflict-related translation keys
