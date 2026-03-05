---
description: >
  Use when the user asks to implement synchronization of a ThingsBoard entity
  between ThingsBoard Server (Cloud) and ThingsBoard Edge. This includes adding
  processors, protobuf messages, event listeners, fetchers, and black box tests.
user_invocable: true
---

# Implementing Edge-Cloud Entity Synchronization

## Overview

ThingsBoard Server and ThingsBoard Edge communicate via gRPC using protobuf messages.
Sync is **bidirectional**:
- **Downlink** (Server → Edge): Server detects entity changes via `EdgeEventSourcingListener`, creates `EdgeEvent`, which is converted to a `DownlinkMsg` protobuf and sent via gRPC.
- **Uplink** (Edge → Server): Edge detects entity changes via `CloudEventSourcingListener`, creates `CloudEvent`, which is converted to an `UplinkMsg` protobuf and sent via gRPC.

ThingsBoard Server is the **upstream** repository — Server code merges into Edge. Therefore:
- `Base*Processor` and `*EdgeProcessor` classes exist in **both** repos (written in Server, inherited by Edge).
- `*CloudProcessor` classes exist **only** in the Edge repo.

## Project Locations

All paths are relative to this project (`tb-ce`) root unless otherwise noted. Sibling projects must be cloned as directories next to this project.

**Expected directory layout:**
```
<parent-dir>/
├── tb-ce/                  # This project (ThingsBoard Server CE)
├── tb-edge-ce/             # ThingsBoard Edge CE
├── edge-black-box-test/    # Edge Black Box Tests CE
├── tb-pe/                  # ThingsBoard Server PE
├── tb-edge-pe/             # ThingsBoard Edge PE
└── edge-black-box-test-pe/ # Edge Black Box Tests PE
```

**Before starting, verify that required sibling directories exist.** Not all are needed for every task — CE-only work needs only the CE projects, PE work needs all. Check which ones exist and tell the user if any required project is missing.

| Project | Path | Purpose |
|---------|------|---------|
| ThingsBoard Server CE | `.` (this project) | Cloud-side sync logic (CE) |
| ThingsBoard Edge CE | `../tb-edge-ce` | Edge-side sync logic (CE) |
| Edge Black Box Tests CE | `../edge-black-box-test` | Integration tests (CE) |
| ThingsBoard Server PE | `../tb-pe` | Cloud-side sync logic (PE) |
| ThingsBoard Edge PE | `../tb-edge-pe` | Edge-side sync logic (PE) |
| Edge Black Box Tests PE | `../edge-black-box-test-pe` | Integration tests (PE) |

### Merge Directions (upstream → downstream)
- `tb-ce` → `tb-pe` (CE server merges into PE server)
- `tb-pe` → `tb-edge-pe` (PE server merges into PE edge)
- `tb-edge-ce` → `tb-edge-pe` (CE edge merges into PE edge)
- `edge-black-box-test` → `edge-black-box-test-pe` (CE BBT merges into PE BBT)

**Implementation order:** CE first, then PE. PE inherits most CE code via merges, so PE changes are mostly the same files at the same relative paths. PE has additional entity types in switch statements (e.g., `SCHEDULER_EVENT`, `REPORT_TEMPLATE`, `ROLE`, `SECRET`) — insert the new type alongside existing entries.

## Reference Diffs (read these to understand exact patterns before implementing)

- **Server diff (Calculated Fields):** `.claude/skills/reference-diffs/server-calculated-field-sync.diff`
- **Edge diff (Calculated Fields):** `.claude/skills/reference-diffs/edge-calculated-field-sync.diff`
- **Black Box Tests diff (Calculated Fields):** `.claude/skills/reference-diffs/bbt-calculated-field-sync.diff`

Original PR URLs (for manual browser review):
- Server: https://github.com/thingsboard/thingsboard/pull/13494
- Edge: https://github.com/thingsboard/thingsboard-edge/pull/172
- Black Box Tests: https://github.com/thingsboard/edge-black-box-test/pull/4

---

## Entity Sync Patterns

There are two main patterns for entity synchronization:

### Pattern A: Standalone Entity Sync (e.g., CalculatedField, AiModel, Device)
The entity has its own `EdgeEventType`, its own processor, and is synced independently. The generic `EdgeEventSourcingListener`/`CloudEventSourcingListener` flow handles event detection automatically via `EntityType` → `EdgeEventType` mapping.

### Pattern B: Child Entity Piggybacking on Parent Sync (e.g., ApiKey → User)
The entity belongs to a parent (e.g., ApiKey belongs to User). It gets its own `EdgeEventType`, processor, and protobuf messages, but its **sync is driven by the parent**:

**When to use `allEdgesRelated=true` vs `false`:**
- If the child entity's parent is tenant-wide (e.g., User), set `allEdgesRelated=true` on the child's `EdgeEventType`. This lets the **generic** `EdgeEventSourcingListener` flow handle save/delete events automatically — no early interception or `processEntityNotification` override needed.
- If the child entity's parent is edge-specific (not tenant-wide), set `allEdgesRelated=false` and use early interception (see variant below).

#### Pattern B with `allEdgesRelated=true` (e.g., ApiKey → User)

1. **Event handling is automatic** — the generic `EdgeEventSourcingListener`/`CloudEventSourcingListener` flow handles the child entity's `SaveEntityEvent`/`DeleteEntityEvent` via `EntityType` → `EdgeEventType` mapping, same as Pattern A. No early interception needed. No `processEntityNotification` override needed.

2. **Parent processor includes child entities during parent sync** — in the parent's `convertEdgeEventToDownlink` (server) / `convertCloudEventToUplink` (edge), in the ADDED/UPDATED case, after building the parent message, also fetch all child entities and add them to the **same** `DownlinkMsg`/`UplinkMsg`:
   ```java
   // In UserEdgeProcessor.convertEdgeEventToDownlink(), ADDED/UPDATED case:
   // After adding UserUpdateMsg and UserCredentialsUpdateMsg...
   List<ApiKey> apiKeys = edgeCtx.getApiKeyService().findApiKeysByUserId(edgeEvent.getTenantId(), userId);
   for (ApiKey apiKey : apiKeys) {
       builder.addApiKeyUpdateMsg(EdgeMsgConstructorUtils.constructApiKeyUpdatedMsg(msgType, apiKey));
   }
   ```
   **Important:** Use the same `msgType` as the parent event (from `getUpdateMsgType(edgeEvent.getAction())`), not a hardcoded value.

3. **No separate edge sync fetcher** — child entities piggyback on parent sync during full sync. **No entry in `EdgeSyncCursor` needed.** When the parent is synced, its processor's `convertEdgeEventToDownlink` includes all child entities.

4. **EdgeGrpcSession: child processing AFTER parent with `sequenceDependencyLock`** — in `processUplinkMsg()`, the child entity's message processing block must be placed **after** the parent's message processing block (e.g., `ApiKeyUpdateMsg` after `UserCredentialsUpdateMsg`). Both parent and child blocks must use `sequenceDependencyLock` to ensure the parent entity is fully created before the child is processed:
   ```java
   // First: UserUpdateMsg with sequenceDependencyLock
   // Then: UserCredentialsUpdateMsg with sequenceDependencyLock
   // Then: ApiKeyUpdateMsg with sequenceDependencyLock
   if (uplinkMsg.getApiKeyUpdateMsgCount() > 0) {
       for (ApiKeyUpdateMsg apiKeyUpdateMsg : uplinkMsg.getApiKeyUpdateMsgList()) {
           sequenceDependencyLock.lock();
           try {
               result.add(ctx.getApiKeyProcessor().processApiKeyMsgFromEdge(edge.getTenantId(), edge, apiKeyUpdateMsg));
           } finally {
               sequenceDependencyLock.unlock();
           }
       }
   }
   ```

5. **Full-set approach on receiver** — when the edge/cloud receives child entity messages, it saves/updates/deletes by entity ID.

6. **Child entity service must publish `DeleteEntityEvent`** — ensure the service's delete method publishes `DeleteEntityEvent` so the generic listener handles it.

7. **Child entity service needs `saveEntity(TenantId, Entity)` overload** — for edge sync, the processor receives the full entity with ID and `value` already set. Add an overload that saves without regenerating secrets/values and without full validation (the data was already validated on the sending side). Check existence via `dao.findById()` directly:
   ```java
   public ApiKey saveApiKey(TenantId tenantId, ApiKey apiKey) {
       ApiKey old = apiKey.getId() != null ? apiKeyDao.findById(tenantId, apiKey.getUuidId()) : null;
       if (old != null && apiKey.getValue() == null) {
           apiKey.setValue(old.getValue()); // preserve existing secret
       }
       var saved = apiKeyDao.save(tenantId, apiKey);
       eventPublisher.publishEvent(SaveEntityEvent.builder()...created(old == null).build());
       return saved;
   }
   ```

#### Pattern B with `allEdgesRelated=false` (edge-specific parent)

When the parent entity is NOT tenant-wide, early interception IS needed:

1. **Event listeners use early interception** — `EdgeEventSourcingListener`/`CloudEventSourcingListener` intercept save/delete events for the child entity BEFORE the generic flow and re-route them with the child's **entityId**:
   ```java
   // In handleEvent(SaveEntityEvent), before generic flow:
   if (entity instanceof ChildEntity child) {
       tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, child.getId(),
           null, EdgeEventType.CHILD_ENTITY, EdgeEventActionType.UPDATED,
           edgeSynchronizationManager.getEdgeId().get());
       return;
   }
   ```
   Same pattern in `CloudEventSourcingListener` using `sendNotificationMsgToCloud`.

2. **`processEntityNotification` is overridden** — to find related edges via the parent entity:
   ```java
   @Override
   public ListenableFuture<Void> processEntityNotification(TenantId tenantId,
           TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
       // Fetch child entity, get parent ID, find related edges via parent
       ChildEntityId entityId = new ChildEntityId(new UUID(
           edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
       ParentId parentId = edgeCtx.getChildService().findById(entityId).getParentId();
       return processNotificationToRelatedEdges(tenantId, parentId, entityId,
           type, actionType, originatorEdgeId);
   }
   ```

3. All other points (parent processor includes children, no fetcher, sequenceDependencyLock in EdgeGrpcSession, etc.) are the same as the `allEdgesRelated=true` variant above.

---

## Step-by-Step Implementation Guide

### Step 1: Analyze the Entity

Before writing code, understand:
- What entity fields need to be synced?
- Is this entity `allEdgesRelated` (synced to ALL edges in a tenant) or only to specific assigned edges?
- Does the entity have an owner (device/asset) that determines edge assignment?
- Does the entity need a dedicated fetcher for full sync, or is it synced via relations?

### Step 2: Extend EdgeEventType enum (Server project)

**File:** `common/data/src/main/java/org/thingsboard/server/common/data/edge/EdgeEventType.java`

Add new entry:
```java
NEW_ENTITY(true/false, EntityType.NEW_ENTITY),
```
- First param `allEdgesRelated`: `true` = sync to all edges in tenant (e.g., profiles, settings); `false` = only related edges.
- Second param: the matching `EntityType` enum value, or `null` for metadata-only types.

### Step 3: Extend EntityIdFactory (Server project)

**File:** `common/data/src/main/java/org/thingsboard/server/common/data/id/EntityIdFactory.java`

Add case in **two** methods:
1. `getByTypeAndUuid(EntityType, UUID)` (if not already present):
   ```java
   case NEW_ENTITY -> new NewEntityId(uuid);
   ```
2. `getByEdgeEventTypeAndUuid(EdgeEventType, UUID)`:
   ```java
   case NEW_ENTITY -> new NewEntityId(uuid);
   ```

**CRITICAL:** The `getByEdgeEventTypeAndUuid` method uses a switch with `default -> throw IllegalArgumentException`. Forgetting this case causes `IllegalArgumentException: EdgeEventType NEW_ENTITY is not supported!` at runtime when edge events are processed via `BaseEdgeProcessor.processEntityNotification`.

### Step 4: Add protobuf message definition (Server project)

**File:** `common/edge-api/src/main/proto/edge.proto`

1. Define the update message:
   ```proto
   message NewEntityUpdateMsg {
     UpdateMsgType msgType = 1;
     int64 idMSB = 2;
     int64 idLSB = 3;
     string entity = 4;
   }
   ```

2. Add field to `UplinkMsg` (next available field number):
   ```proto
   repeated NewEntityUpdateMsg newEntityUpdateMsg = <NEXT_NUMBER>;
   ```

3. Add field to `DownlinkMsg` (next available field number):
   ```proto
   repeated NewEntityUpdateMsg newEntityUpdateMsg = <NEXT_NUMBER>;
   ```

After modifying proto, regenerate Java classes:
```bash
mvn generate-sources -pl common/edge-api
```

### Step 5: Add Processor classes (Server project)

**Package:** `org.thingsboard.server.service.edge.rpc.processor`

Create a new sub-package (e.g., `newentity/`) under:
`application/src/main/java/org/thingsboard/server/service/edge/rpc/processor/`

#### 5a. Create the Processor interface

**File:** `NewEntityProcessor.java`
```java
public interface NewEntityProcessor extends EdgeProcessor {
    // Define entity-specific processing methods if needed
}
```

#### 5b. Create BaseNewEntityProcessor

**File:** `BaseNewEntityProcessor.java`
- Extends `BaseEdgeProcessor`
- Contains **shared** save/update logic used by both Server and Edge
- Key method: `saveOrUpdateNewEntity(TenantId, NewEntityId, NewEntityUpdateMsg)`
- This method should save the entity **without validation** (validation happens before sync)
- Use `JacksonUtil.fromString(msg.getEntity(), NewEntity.class)` to deserialize

#### 5c. Create NewEntityEdgeProcessor

**File:** `NewEntityEdgeProcessor.java`
- Extends `BaseNewEntityProcessor`
- Implements `NewEntityProcessor`
- Annotated with `@Component` and `@TbCoreComponent`
- Key methods:
  - `processNewEntityMsgFromEdge(TenantId, Edge, NewEntityUpdateMsg)` — handles uplink messages
  - `convertEdgeEventToDownlink(EdgeEvent, EdgeVersion)` — converts edge events to `DownlinkMsg`
  - `getEdgeEventType()` — returns `EdgeEventType.NEW_ENTITY`

**Reference:** Look at existing processors like `CalculatedFieldEdgeProcessor` or `AiModelEdgeProcessor` in the same directory for the exact pattern.

### Step 6: Update EdgeMsgConstructorUtils (Server project)

**File:** `application/src/main/java/org/thingsboard/server/service/edge/EdgeMsgConstructorUtils.java`

Add two static methods:
```java
public static NewEntityUpdateMsg constructNewEntityUpdatedMsg(UpdateMsgType msgType, NewEntity entity) {
    return NewEntityUpdateMsg.newBuilder()
            .setMsgType(msgType)
            .setIdMSB(entity.getId().getId().getMostSignificantBits())
            .setIdLSB(entity.getId().getId().getLeastSignificantBits())
            .setEntity(JacksonUtil.toString(entity))
            .build();
}

public static NewEntityUpdateMsg constructNewEntityDeleteMsg(NewEntityId entityId) {
    return NewEntityUpdateMsg.newBuilder()
            .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
            .setIdMSB(entityId.getId().getMostSignificantBits())
            .setIdLSB(entityId.getId().getLeastSignificantBits())
            .build();
}
```

Also check `EXCLUDED_NODES_BY_EDGE_VERSION` — if older edge versions don't support this entity, add version exclusions.

### Step 7: Update EdgeEventSourcingListener (Server project)

**File:** `application/src/main/java/org/thingsboard/server/service/edge/EdgeEventSourcingListener.java`

**For standalone entities (Pattern A):** Update these methods to handle the new entity type:
1. **`isValidSaveEntityEventForEdgeProcessing()`** — return `true` for the new entity type (or add filtering logic)
2. **`getEdgeEventTypeForEntityEvent()`** — map entity class to `EdgeEventType.NEW_ENTITY`
3. **`getBodyMsgForEntityEvent()`** — construct the body (usually the entity ID or owner ID)
4. **`getActionForEntityEvent()`** — map to `ADDED`/`UPDATED` action

**For child entities piggybacking on parent (Pattern B):**
- If `allEdgesRelated=true`: **No changes needed.** The generic flow handles the child entity automatically via `EntityType` → `EdgeEventType` mapping.
- If `allEdgesRelated=false`: Add **early interception** in both `handleEvent(SaveEntityEvent)` and `handleEvent(DeleteEntityEvent)` BEFORE the generic flow. The interception re-routes the event with the entityId and uses `UPDATED`/`DELETED` actions. See "Pattern B with `allEdgesRelated=false`" section above for exact code.

### Step 8: Update EdgeContextComponent + BaseEdgeProcessor (Server project)

**File:** `application/src/main/java/org/thingsboard/server/service/edge/EdgeContextComponent.java`
- Add `@Autowired` for the new entity's service (if not already present)
- Add `@Autowired` for the new processor interface
- The processor is auto-registered in `processorMap` via Spring `@Component` scanning + `getEdgeEventType()`

**File:** `application/src/main/java/org/thingsboard/server/service/edge/rpc/processor/BaseEdgeProcessor.java`
- Add new `EdgeEventType` to `doSaveIfEdgeIsOffline()` type list if events should be saved when edge is offline

### Step 9: Update EdgeGrpcSession (Server project)

**File:** `application/src/main/java/org/thingsboard/server/service/edge/rpc/EdgeGrpcSession.java`

In `processUplinkMsg()`, add handling block for the new entity's uplink messages:

**For Pattern A (standalone):** Add block anywhere in the method (after existing blocks).

**For Pattern B (child entity):** The child's processing block must be placed **AFTER** the parent's processing block and must use `sequenceDependencyLock`. This ensures the parent entity (e.g., User) is fully created before the child (e.g., ApiKey) is processed. Example ordering: `UserUpdateMsg` → `UserCredentialsUpdateMsg` → `ApiKeyUpdateMsg`, all using `sequenceDependencyLock`.

### Step 10: Update EdgeSyncCursor (Server project, Pattern A only)

**File:** `application/src/main/java/org/thingsboard/server/service/edge/rpc/EdgeSyncCursor.java`

**For Pattern A (standalone):** Create a new `EdgeEventFetcher` and add it to the fetcher list.

**Fetcher location:** `application/src/main/java/org/thingsboard/server/service/edge/rpc/fetch/`

- Create `NewEntityEdgeEventFetcher.java` extending `BasePageableEdgeEventFetcher`
- Add it to the fetcher list in `EdgeSyncCursor` constructor at the appropriate position
- Note: Some entities (like CalculatedField) don't need a dedicated fetcher if they're synced via owner relations.

**For Pattern B (child entity): Skip this step entirely.** Child entities do NOT get their own fetcher or entry in `EdgeSyncCursor`. They are synced automatically when the parent entity is synced — the parent's `convertEdgeEventToDownlink` includes all child entities in the same `DownlinkMsg`.

### Step 11: Add service method for edge sync saving (Server project)

In the entity's service class, add an overloaded save method that the processor can call. Key requirements:
- **Skip validator `validateUpdate()`** — for edge sync creates where the entity has an ID set but doesn't exist in DB yet, `validateUpdate()` would throw "Cannot update non-existent entity". Instead, check existence directly via `dao.findById()`.
- **Preserve secrets/tokens** — if the entity has a generated secret (like `ApiKey.value`), the overload must accept it from the incoming data (for sync) but also preserve the old value if null (for regular updates).
- **Publish `SaveEntityEvent`** with correct `created` flag based on existence check.
- **Publish `DeleteEntityEvent`** in the delete method — without this, edge sync delete notifications won't fire.
- **Add `deleteEntity(TenantId, EntityId, boolean)` override** from `EntityDaoService` for consistent delete handling.

### Step 12: Add test class - NewEntityEdgeTest (Server project)

**File:** `application/src/test/java/org/thingsboard/server/edge/NewEntityEdgeTest.java`

- Create new class that extends `AbstractEdgeTest`
- Add standard test methods:
  - `testNewEntity_create_update_delete_fromCloud()` — verify server CRUD triggers edge messages
  - `testNewEntity_create_update_delete_toCloud()` — verify edge uplink messages persist on server
- **For Pattern B (child entity), also add:**
  - `testNewEntity_pushedDuringParentSync()` — create parent entity, create child entity, then update parent. Verify that the parent update message includes the child entity message in the same batch. Example for ApiKey→User: create user (3 msgs), create API key (1 msg), update user → expect 3 msgs (UserUpdateMsg + UserCredentialsUpdateMsg + ApiKeyUpdateMsg). Use `edgeImitator.findAllMessagesByType()` to verify exact counts per message type.
- If the entity has no REST GET endpoint, use `@Autowired EntityService` directly for assertions instead of `doGet()`

### Step 13: Update EdgeImitator (Server project)

**File:** `application/src/test/java/org/thingsboard/server/edge/imitator/EdgeImitator.java`

- Add handling for the new protobuf message type in the imitator's message collection

---

## ThingsBoard Edge Project Changes

All Edge-specific changes go in `../tb-edge-ce`.

### Step 14: Add CloudEventType enum entry + Extend EntityIdFactory (Edge project)

**File:** `../tb-edge-ce/common/data/src/main/java/org/thingsboard/server/common/data/cloud/CloudEventType.java`

Add new entry:
```java
NEW_ENTITY(EntityType.NEW_ENTITY),
```

**File:** `../tb-edge-ce/common/data/src/main/java/org/thingsboard/server/common/data/id/EntityIdFactory.java`

Add case in **two** methods (mirrors Step 3 for edge):
1. `getByEdgeEventTypeAndUuid(EdgeEventType, UUID)`:
   ```java
   case NEW_ENTITY -> new NewEntityId(uuid);
   ```
2. `getByCloudEventTypeAndUuid(CloudEventType, UUID)`:
   ```java
   case NEW_ENTITY -> new NewEntityId(uuid);
   ```

**CRITICAL:** Both switch methods use `default -> throw IllegalArgumentException`. Forgetting either causes runtime errors when edge/cloud events are processed.

### Step 15: Create CloudProcessor (Edge project only)

**File:** `../tb-edge-ce/application/src/main/java/org/thingsboard/server/service/cloud/rpc/processor/NewEntityCloudProcessor.java`

- Extends `BaseNewEntityProcessor` (inherited from Server)
- Annotated with `@Component`
- Key methods:
  - `processNewEntityMsgFromCloud(TenantId, NewEntityUpdateMsg)` — handles downlink messages from cloud
  - `convertCloudEventToUplink(CloudEvent)` — converts cloud events to `UplinkMsg`
  - `getCloudEventType()` — returns `CloudEventType.NEW_ENTITY`
- Must use `cloudSynchronizationManager.getSync().set(true)` in try/finally to prevent recursive sync

**Reference pattern (CalculatedFieldCloudProcessor):**
```java
public ListenableFuture<Void> processNewEntityMsgFromCloud(TenantId tenantId, NewEntityUpdateMsg msg) {
    NewEntityId entityId = new NewEntityId(new UUID(msg.getIdMSB(), msg.getIdLSB()));
    try {
        cloudSynchronizationManager.getSync().set(true);
        return switch (msg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                saveOrUpdateNewEntity(tenantId, entityId, msg);
                yield Futures.immediateFuture(null);
            }
            case ENTITY_DELETED_RPC_MESSAGE -> {
                // delete entity
                yield Futures.immediateFuture(null);
            }
            default -> handleUnsupportedMsgType(msg.getMsgType());
        };
    } finally {
        cloudSynchronizationManager.getSync().remove();
    }
}
```

### Step 16: Update CloudEventSourcingListener (Edge project)

**File:** `../tb-edge-ce/application/src/main/java/org/thingsboard/server/service/cloud/CloudEventSourcingListener.java`

**For standalone entities (Pattern A):**
- Add the new entity type to `baseEventSupportableEntityTypes` set
- Update `getCloudEventTypeForEntityEvent()` to map the entity class to `CloudEventType`
- Update `getBodyMsgForEntityEvent()` and `getActionForEntityEvent()` if needed

**For child entities piggybacking on parent (Pattern B):**
- If `allEdgesRelated=true`: Add `EntityType.NEW_ENTITY` to `COMMON_ENTITY_TYPES` list. The generic flow handles the rest.
- If `allEdgesRelated=false`: Add **early interception** in both `handleEvent(SaveEntityEvent)` and `handleEvent(DeleteEntityEvent)` BEFORE the entity type check. The interception re-routes the event with the entityId using `sendNotificationMsgToCloud`. See "Pattern B with `allEdgesRelated=false`" section above for exact code.

### Step 16b: Update DefaultCloudNotificationService (Edge project)

**File:** `../tb-edge-ce/application/src/main/java/org/thingsboard/server/service/cloud/DefaultCloudNotificationService.java`

**CRITICAL:** The `pushNotificationToCloud()` method has a `switch (cloudEventType)` that routes entity types to `processEntity()`. **You MUST add the new `CloudEventType` to this switch.** Without it, the cloud event is silently dropped (logged as warning "Cloud event type [X] is not designed to be pushed to cloud") and the uplink message is never created — causing edge→cloud sync to silently fail.

Add to the existing entity types case:
```java
case EDGE, ASSET, DEVICE, ..., AI_MODEL, NEW_ENTITY ->
        future = processEntity(tenantId, cloudNotificationMsg);
```

### Step 17: Update UplinkMsgMapper (Edge project)

**File:** `../tb-edge-ce/application/src/main/java/org/thingsboard/server/service/cloud/event/UplinkMsgMapper.java`

- Ensure the new entity's `CloudEventType` is routed to the correct processor
- The main switch in `convertCloudEventToUplink()` routes by action type — entity CRUD actions go through `convertEntityEventToUplink()` which uses `cloudCtx.getProcessor(cloudEvent.getType())`
- If the entity has special actions (like `CALCULATED_FIELD_REQUEST`), add a dedicated case

### Step 18: Update DefaultDownlinkMessageService (Edge project)

**File:** `../tb-edge-ce/application/src/main/java/org/thingsboard/server/service/cloud/DefaultDownlinkMessageService.java`

- Add handling for the new message type in downlink processing
- Route `DownlinkMsg.newEntityUpdateMsg` to `NewEntityCloudProcessor.processNewEntityMsgFromCloud()`

### Step 19: Update CloudContextComponent (Edge project)

**File:** `../tb-edge-ce/application/src/main/java/org/thingsboard/server/service/cloud/CloudContextComponent.java`

- Add `@Autowired` for the new CloudProcessor

---

## Edge Black Box Tests

All test changes go in `../edge-black-box-test`.

### Step 20: Create Black Box Test (Black Box Tests project)

**File:** `../edge-black-box-test/src/test/java/org/thingsboard/server/msa/edge/NewEntityClientTest.java`

**Base class:** `AbstractContainerTest` at `../edge-black-box-test/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`

**Pattern:**
```java
@Slf4j
public class NewEntityClientTest extends AbstractContainerTest {

    @Test
    public void testSendNewEntityToEdge() {
        performTestOnEachEdge(this::_testSendNewEntityToEdge);
    }

    @Test
    public void testSendNewEntityToCloud() {
        performTestOnEachEdge(this::_testSendNewEntityToCloud);
    }

    private void _testSendNewEntityToEdge() {
        // 1. Create entity on cloud
        // 2. Assign to edge (if not allEdgesRelated)
        // 3. Await sync to edge using Awaitility (poll 500ms, max 30s)
        // 4. Update entity on cloud
        // 5. Await update on edge
        // 6. Delete entity on cloud
        // 7. Await deletion on edge
        // 8. Clean up
    }

    private void _testSendNewEntityToCloud() {
        // 1. Create entity on edge
        // 2. Await sync to cloud using Awaitility
        // 3. Update entity on edge
        // 4. Await update on cloud
        // 5. Delete entity on edge
        // 6. Await deletion on cloud
        // 7. Clean up
    }
}
```

**Key utilities from AbstractContainerTest:**
- `cloudRestClient` / `edgeRestClient` — REST clients for each side
- `performTestOnEachEdge(Runnable)` — runs test against each configured edge version
- `Awaitility.await().pollInterval(500, MILLISECONDS).atMost(30, SECONDS).until(...)` — async verification pattern
- `minEdgeVersion` — set to skip test on older edge versions

---

## Checklist Summary

### Server project (this project, `tb-ce`)

#### Common (both Pattern A and B)
- [ ] `EdgeEventType` — add new enum entry (`allEdgesRelated=true` for tenant-wide / child-of-tenant-wide entities)
- [ ] `EntityIdFactory` — add cases in `getByTypeAndUuid()` (if needed) AND `getByEdgeEventTypeAndUuid()` (**CRITICAL: forgetting causes runtime `IllegalArgumentException`**)
- [ ] `edge.proto` — add message, UplinkMsg field, DownlinkMsg field
- [ ] `BaseNewEntityProcessor` — shared save/update logic
- [ ] `NewEntityEdgeProcessor` — server-side processor (`@Component`)
- [ ] `NewEntityProcessor` interface
- [ ] `EdgeMsgConstructorUtils` — add construct/delete msg methods
- [ ] `EdgeContextComponent` — add `@Autowired` for new service and processor
- [ ] `BaseEdgeProcessor.doSaveIfEdgeIsOffline()` — add new type if offline saving needed
- [ ] Entity service — add edge sync save overload (skip validator, preserve secrets); ensure `DeleteEntityEvent` in delete; add `deleteEntity()` override
- [ ] `EdgeImitator` — handle new message type
- [ ] `NewEntityEdgeTest` — create/update/delete from cloud + to cloud tests

#### Pattern A only (standalone entity)
- [ ] `EdgeEventSourcingListener` — update generic methods to handle new entity type
- [ ] `EdgeGrpcSession` — add uplink processing block (no ordering constraint)
- [ ] `EdgeSyncCursor` + `NewEntityEdgeEventFetcher` — for full sync

#### Pattern B only (child entity piggybacking on parent)
- [ ] `EdgeGrpcSession` — add uplink processing block **AFTER parent's block** with `sequenceDependencyLock`
- [ ] Parent processor (`convertEdgeEventToDownlink`) — fetch all child entities and add to same `DownlinkMsg` in ADDED/UPDATED case
- [ ] `NewEntityEdgeTest` — add `testNewEntity_pushedDuringParentSync()` test (create parent, create child, update parent → verify child msg included)
- [ ] **NO** `EdgeSyncCursor` entry or fetcher needed
- [ ] If `allEdgesRelated=true`: **NO** `EdgeEventSourcingListener` changes needed (generic flow handles it)
- [ ] If `allEdgesRelated=false`: Add early interception in `EdgeEventSourcingListener` + override `processEntityNotification` in processor

### Edge project (`../tb-edge-ce`)

#### Common (both Pattern A and B)
- [ ] `CloudEventType` — add new enum entry
- [ ] `EntityIdFactory` — add cases in `getByEdgeEventTypeAndUuid()` AND `getByCloudEventTypeAndUuid()` (**CRITICAL: forgetting causes runtime `IllegalArgumentException`**)
- [ ] `NewEntityCloudProcessor` — edge-side processor
- [ ] `CloudContextComponent` — add `@Autowired` for new processor
- [ ] `DefaultCloudNotificationService.pushNotificationToCloud()` — add new `CloudEventType` to the entity types switch (**CRITICAL: forgetting causes edge→cloud sync to silently fail — events are dropped with only a warning log**)
- [ ] `UplinkMsgMapper` — route new event type (usually automatic via `convertEntityEventToUplink`)
- [ ] `DefaultDownlinkMessageService` — handle new downlink message

#### Pattern A only
- [ ] `CloudEventSourcingListener` — add to `COMMON_ENTITY_TYPES` (which feeds both `baseEventSupportableEntityTypes` and `supportableEntityTypes`), update mapping methods

#### Pattern B only (child entity piggybacking on parent)
- [ ] `CloudEventSourcingListener` — add `EntityType.NEW_ENTITY` to `COMMON_ENTITY_TYPES` list (required for the generic flow to handle save/delete events, regardless of `allEdgesRelated` value)
- [ ] Parent cloud processor — update `convertCloudEventToUplink` to include child entities alongside parent
- [ ] If `allEdgesRelated=false`: Add early interception in `CloudEventSourcingListener`

### Black Box Tests CE (`../edge-black-box-test`)
- [ ] `NewEntityClientTest` — cloud→edge and edge→cloud sync tests

---

## PE Projects

PE projects mirror CE with the same file structure. Apply the **same steps** as CE, just at the PE paths. Key differences:

- PE switch statements have additional entity types (`SCHEDULER_EVENT`, `REPORT_TEMPLATE`, `ROLE`, `SECRET`, etc.) — insert new type alongside them
- PE `DefaultCloudNotificationService` switch may include PE-specific types — add new type to the same case
- PE BBT `AbstractContainerTest` has PE-specific helpers (entity groups, customer ownership) — not needed for tenant-wide entities

### Server PE (`../tb-pe`)
Same checklist as Server CE — all items apply identically at PE paths.

### Edge PE (`../tb-edge-pe`)
Same checklist as Edge CE — all items apply identically at PE paths, **plus:**

- [ ] `RoleCloudProcessor.replaceWriteOperationsToReadIfRequired()` — **CRITICAL PE-only step.** This method processes role permissions when they arrive from cloud to edge. It has a `switch (entry.getKey())` on `Resource` that determines which operations are preserved for each resource type on the edge. **You MUST add the new `Resource.NEW_ENTITY` to the appropriate case.** Without it, the resource falls to `default` which strips `CREATE` and other write operations, causing **403 "You don't have permission to perform 'CREATE' operation"** errors on the edge.
  - For entities that need full CRUD on edge (like `API_KEY`, `AI_MODEL`, `USER`): add to the case group at line ~165 that passes through all original operations (`newOperations = entry.getValue()`)
  - For entities that only need read on edge: add to the appropriate restricted case group
  - **Also update the `Resource.ALL` expansion block** (~line 189): add `newPermissions.put(Resource.NEW_ENTITY, Collections.singletonList(Operation.ALL))` so that roles with `ALL` permissions include the new resource
  - **File:** `../tb-edge-pe/application/src/main/java/org/thingsboard/server/service/cloud/rpc/processor/RoleCloudProcessor.java`

### Black Box Tests PE (`../edge-black-box-test-pe`)
- [ ] `NewEntityClientTest` — cloud→edge and edge→cloud sync tests (same as CE)

### Reference PE Diffs (read these to understand exact patterns before implementing)

- **Server PE diff (Calculated Fields):** `.claude/skills/reference-diffs/server-calculated-field-sync-pe.diff`
- **Edge PE diff (Calculated Fields):** `.claude/skills/reference-diffs/edge-calculated-field-sync-pe.diff`
- **Black Box Tests PE diff (Calculated Fields):** `.claude/skills/reference-diffs/bbt-calculated-field-sync-pe.diff`
