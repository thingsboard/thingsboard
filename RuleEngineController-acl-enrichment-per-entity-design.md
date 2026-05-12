# Rule Engine ACL Enrichment — Per-Entity Permissions (Design v2)

**Date:** 2026-05-12
**Author:** Oleksandra Matviienko
**Status:** Approved for implementation; implementation merged in this PR
**Scope:** ThingsBoard CE (branch: based on `lts-4.3`)
**Supersedes:** the role-level design (issue #15496 / prior `RuleEngineController-acl-enrichment-design.md`)

---

## 1. Problem Statement

When the REST API pushes a message to the Rule Engine, the engine runs under effectively-root privileges. Rule chains can read, write, or delete any entity without knowing whether the *calling user* was actually authorized to do so. The only permission check happens in the controller against a single originator entity (`Operation.WRITE`); once the `TbMsg` enters the queue, user identity and authorization context are lost.

This prevents rule-chain authors from building flows whose behavior depends on the caller's permissions — e.g., "allow this action only if the caller can WRITE to asset X", or "log who initiated this action".

## 2. Goal

Add an **optional, additive, non-breaking** enrichment path to the Rule Engine REST API that:

1. Accepts a list of entities from the caller alongside the payload.
2. Computes, server-side, **per-entity** — the set of operations the calling user is allowed to perform on each *specific* entity instance.
3. Writes that ACL snapshot into **protected** `TbMsg` metadata keys the caller cannot override.
4. Also writes the caller's user id, for audit/logging inside rule chains.
5. Forwards the enriched `TbMsg` to the Rule Engine as usual.

Rule chains are free to ignore the metadata entirely — existing flows are unaffected.

## 3. Non-Goals

- Denying the request at the API layer based on the ACL snapshot. Decisions live in the rule chain.
- Modifying or replacing the existing `POST /api/rule-engine/...` v1 endpoints. This adds new `/v2` endpoint alongside them.
- Changing `AccessValidator`, `AccessControlService`, `Resource`, `Operation`, or permission-model semantics. We consume them as-is.
- UI changes.
- Extending enrichment to controllers other than `RuleEngineController` (see §13).

## 4. API Contract

A single new endpoint with all routing parameters in the request body:

- `POST /api/rule-engine/v2`

Same authorization as v1: `hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')` and existing `AccessValidator` WRITE check on the originator.

### Request body

```json
{
  "originator":   { "entityType": "DEVICE", "id": "784f394c-42b6-..." },
  "queueName":    "Main",
  "timeout":      10000,
  "payload":      { "any": "user json" },
  "enrichEntities": [
    { "entityType": "DEVICE", "id": "784f394c-42b6-..." },
    { "entityType": "ASSET",  "id": "abc-123-..." }
  ]
}
```

- `originator` — optional `EntityId`. If absent, defaults to the calling user's id (matches v1 behavior when path variables are omitted).
- The `TbMsg` type is hardcoded to `REST_API_REQUEST` (matches v1 across all four ruleEngine endpoints; this is the documented input type for the terminal `rest call reply` rule node, see https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/rest-call-reply/).
- `queueName` — optional. When present, overrides the queue selected by the originator's profile (same semantics as v1).
- `timeout` — optional, milliseconds. Defaults to `server.rest.rule_engine.response_timeout` (10s).
- `payload` — optional JSON. A null or missing payload is treated as the empty JSON object `{}` so probe-only requests (callers who want only the ACL snapshot) work without a body.
- `enrichEntities` — optional list of `EntityId` (uses the platform's existing polymorphic `EntityId` Jackson representation: `{entityType, id}`). If absent or empty, `tb_aclSnapshot` is `"[]"`.
- Size limit: configurable via `server.rest.rule_engine.acl.max_entities` (default **20**). Exceeding returns `400 Bad Request`.

### Response

Unchanged from v1: `DeferredResult<ResponseEntity>` populated by the Rule Engine's terminal "REST Call Reply" node (or `408`/`504` on timeout).

## 5. Protected Metadata

Two new keys, both **always written last** when building `TbMsgMetaData`, so any user-supplied value (if a caller tried to sneak them in via the payload or elsewhere) is unconditionally overwritten.

### 5.1 `tb_aclSnapshot`

- Key constant: `TbMsgMetaData.TB_ACL_KEY = "tb_aclSnapshot"`
- Value: JSON string (serialized `List<EntityAclEntry>`).
- Order: matches the order of `enrichEntities` in the request (deterministic).
- Duplicate `EntityId` values in `enrichEntities` produce duplicate entries in the output in the same positions; the underlying entity is loaded only once (dedup cache per request).
- Shape:

```json
[
  {
    "entityType": "DEVICE",
    "entityId": "784f394c-42b6-...",
    "allowed": ["READ", "WRITE", "READ_ATTRIBUTES", "WRITE_ATTRIBUTES", "READ_TELEMETRY"]
  },
  {
    "entityType": "ASSET",
    "entityId": "abc-123-...",
    "allowed": []
  }
]
```

- **`allowed` — per-entity capability**: names from the `Operation` enum the caller can perform on *this specific entity instance* (not on the resource type in the abstract). For Customer Users whose access depends on entity assignment, this correctly reflects whether the caller can act on the concrete entity.
- An empty `allowed` list means either: (a) the user has no operations on this entity, (b) the entity was not found / cross-tenant / not `HasTenantId`, or (c) the `EntityType` has no `Resource` mapping. Server-side WARN logs distinguish (b) and (c) for operations debugging; rule-chain authors can treat empty as a single "no access" signal.
- Format deliberately an array of flat objects (no compound keys like `"DEVICE:uuid"`) so it iterates naturally in JS and TBEL rule nodes.

### 5.2 `tb_userId`

- Key constant: `TbMsgMetaData.TB_USER_ID_KEY = "tb_userId"`
- Value: UUID string of the calling user (`currentUser.getId().getId().toString()`).
- Stable across rename/email changes.
- `tenantId` and `customerId` are already recoverable from `TbMsg` (originator, customerId), so they are not duplicated here.

## 6. Computation Logic

For each `EntityId` in `enrichEntities`, **after** size-limit validation:

1. **Map `EntityType → Resource`** via existing `Resource.of(entityType)`. If it throws `IllegalArgumentException` (no `Resource` for this type — e.g., `RULE_NODE`), log WARN and record the entity with `allowed=[]`. Continue.
2. **Load the entity** via `entityServiceRegistry.getServiceByEntityType(entityType).findEntity(user.getTenantId(), entityId)`:
   - The `tenantId` argument means the DAO-level filter rejects cross-tenant ids → `Optional.empty()`.
   - If `getServiceByEntityType` throws `IllegalArgumentException` (no DAO registered for this type), log WARN, `allowed=[]`, continue.
   - If the result is `Optional.empty()` (entity does not exist, cross-tenant, or was just deleted), log WARN, `allowed=[]`, continue.
   - If the loaded object does not implement `HasTenantId` (rare system-level entity), log WARN, `allowed=[]`, continue.
3. **For each value in `Operation.values()`**:
   - Call `accessControlService.hasPermission(user, resource, op, entityId, entity)` — the **per-entity, boolean-returning** form.
   - `true` → add `op.name()` to `allowed`.
   - `false` → skip silently (normal "you don't have this op" case).
   - Throws `ThingsboardException` → skip silently (the rare "this authority has no checker for this resource" wiring case; defensive catch).
4. Append `new EntityAclEntry(entityType, entityId.getId(), allowed)` to the result list.

### Deduplication

`buildAclMetadata` keeps a `Map<EntityId, EntityAclEntry>` cache for the duration of one request. If `enrichEntities` repeats the same id, the per-entity computation runs once; subsequent occurrences look up the same `EntityAclEntry` reference. The output list preserves duplicates in input order (contract), but DB load and permission probes do not.

### Why per-entity, not role-level

The 3-argument `hasPermission(user, resource, operation)` form asks "does this *authority* permit this op on this *resource type* in general?" — it doesn't take the entity. For `CUSTOMER_USER`, that returns `true` for `WRITE` on **any** `DEVICE`, because the role abstractly grants WRITE on devices. The actual per-instance check (`user.customerId == entity.customerId`) lives in the 5-argument `hasPermission(user, resource, op, entityId, entity)` form. To call it, we must first load the entity object — which is what step 2 above provides via `EntityServiceRegistry`.

This is the core deviation from the original (role-level) design in issue #15496.

### Size limit

`enrichEntities.size() > maxAclEntities` → `ThingsboardException(BAD_REQUEST_PARAMS)` before any entity load.

## 7. Component Layout (Variant A — inline in controller)

### New files

- `common/data/src/main/java/org/thingsboard/server/common/data/rule/engine/EnrichedRuleEngineRequest.java`
  - Fields: `EntityId originator`, `String queueName`, `Integer timeout`, `JsonNode payload`, `List<EntityId> enrichEntities`
  - `@Data` + `@JsonIgnoreProperties(ignoreUnknown = true)`
- `common/data/src/main/java/org/thingsboard/server/common/data/rule/engine/EntityAclEntry.java`
  - Fields: `EntityType entityType`, `UUID entityId`, `List<String> allowed`
  - `@Data @NoArgsConstructor @AllArgsConstructor`
  - Flat shape (not `EntityId`) so JS/TBEL nodes can iterate naturally.

### Modified files

- `common/message/.../TbMsgMetaData.java`
  - Add `public static final String TB_ACL_KEY = "tb_aclSnapshot";`
  - Add `public static final String TB_USER_ID_KEY = "tb_userId";`
- `application/.../controller/RuleEngineController.java`
  - `AccessControlService accessControlService` is already inherited from `BaseController`.
  - Inject `EntityService entityService` — required to load the target entity (the per-entity `hasPermission` form needs the entity object, not just the id). The internal call `entityService.fetchEntity(tenantId, entityId)` dispatches via `EntityServiceRegistry` and returns `Optional<HasId<?>>`.
  - Add `@Value("${server.rest.rule_engine.acl.max_entities:20}") int maxAclEntities;`
  - Add a single `POST /api/rule-engine/v2` endpoint method `handleEnrichedRuleEngineRequest(@RequestBody EnrichedRuleEngineRequest)`.
  - Add private helpers `buildAclMetadata(SecurityUser, List<EntityId>)` and `computeEntry(SecurityUser, EntityId)`. The latter is the per-entity computation described in §6.
  - The forwarded `TbMsg` type is hardcoded to `TbMsgType.REST_API_REQUEST` — matches v1 behavior across all four ruleEngine endpoints. No custom `messageType` input from the body, so the deprecated `TbMsg.TbMsgBuilder.type(String)` form is not needed.
  - The flow: parse `EnrichedRuleEngineRequest` → resolve `originator` (body or current user), `queueName` (body or null), `timeout` (body or `defaultResponseTimeout`), `payload` (body or `{}`) → validate `enrichEntities.size() ≤ maxAclEntities` → existing `AccessValidator` WRITE check on originator → inside `onSuccess()`, compute ACL via `buildAclMetadata(...)` → populate metadata in the order `serviceId, requestUUID, expirationTime, tb_userId, tb_aclSnapshot` → build `TbMsg` with `type = TbMsgType.REST_API_REQUEST` and `data = payloadString` → call `ruleEngineCallService.processRestApiCallToRuleEngine(...)` exactly as today.
- `application/src/main/resources/thingsboard.yml`
  - Add the `acl.max_entities` property under the existing `server.rest.rule_engine` section (where `response_timeout` already lives). The original issue placed it under a top-level `rule-engine:` section, which does not exist in `thingsboard.yml`; co-locating it with `response_timeout` matches the local convention.

### Zero impact on

- v1 endpoints (`POST /api/rule-engine/...`) — untouched.
- `AccessValidator`, `AccessControlService`, `Resource`, `Operation` — consumed only.
- `RuleEngineCallService`, `TbClusterService` — unchanged; they receive a `TbMsg` that simply has extra metadata keys.
- Downstream rule nodes — consume or ignore the new metadata at their discretion.

## 8. Flow Diagram

```
   ┌──────┐  POST /api/rule-engine/v2
   │      │  { originator, queueName, timeout,
   │ User │    payload, enrichEntities:[{entityType,id},...] }
   │      │─────────────────────────┐
   │      │                         │
   └──────┘                         ▼
                        ┌──────────────────────┐
                        │ RuleEngineController │
                        │   /v2 endpoint       │
                        └───────┬──────────────┘
                                │ 1. parse JSON → EnrichedRuleEngineRequest
                                │    (resolve defaults: originator,
                                │     timeout, queueName,
                                │     payload null/missing → "{}")
                                │ 2. validate size ≤ maxAclEntities
                                │ 3. AccessValidator WRITE on originator
                                │    (existing check, unchanged)
                                ▼
                        ┌──────────────────────┐
                        │  buildAclMetadata()  │
                        │  (inline helper)     │
                        └───────┬──────────────┘
                                │ for each entity in input order:
                                │   cache.computeIfAbsent(id, →
                                │     Resource.of(entityType)
                                │     entityServiceRegistry
                                │       .getServiceByEntityType(t)
                                │       .findEntity(tenantId, id)
                                │     for each Operation:
                                │       hasPermission(user, res, op,
                                │                     entityId, entity)
                                │   )
                                │ serialize List<EntityAclEntry>
                                ▼
                        ┌──────────────────────┐
                        │  TbMsgMetaData       │
                        │  serviceId           │
                        │  requestUUID         │
                        │  expirationTime      │
                        │  tb_userId ◀──────── SERVER
                        │  tb_aclSnapshot     ◀──────── OVERWRITE (always last)
                        └───────┬──────────────┘
                                │ build TbMsg(data=payload, metaData=above)
                                ▼
                        ┌──────────────────────┐
                        │ RuleEngineCallService│
                        │ (unchanged)          │
                        └───────┬──────────────┘
                                │ push to queue
                                ▼
                        ┌──────────────────────┐
                        │   Rule Engine        │
                        │   rule chain         │
                        │   script/switch nodes│
                        │   read metadata.tb_aclSnapshot│
                        │   read metadata.tb_userId│
                        │   decide: pass/block │
                        │   → REST Call Reply  │
                        └───────┬──────────────┘
                                │ response TbMsg
                                ▼
                        ┌──────────────────────┐
                        │  Response to User    │
                        │  (200 or whatever    │
                        │   the rule chain set)│
                        └──────────────────────┘
```

## 9. Rule Node Usage Examples

> Entries use a flat `{entityType, entityId, allowed[]}` shape so JS/TBEL nodes don't
> need to know about the polymorphic `EntityId` form.
>
> `allowed` reflects the **per-entity** capability — operations the calling user can
> perform on that specific entity instance. Empty `allowed` means the user has no
> access on the entity (for any reason: no permissions, stale id, unsupported type).

### JS script node — gate by per-entity capability

```js
var acl = JSON.parse(metadata.tb_aclSnapshot);

var canWriteTargetDevice = acl.some(function(e) {
    return e.entityType === 'DEVICE'
        && e.entityId === msg.targetDeviceId
        && e.allowed.indexOf('WRITE') >= 0;
});

var userId = metadata.tb_userId;
metadata.auditActor = userId;

return { msg: msg, metadata: metadata, msgType: canWriteTargetDevice ? 'Allowed' : 'Denied' };
```

### TBEL script node

```
var acl = JSON.parse(metadata.tb_aclSnapshot);
var canWriteTargetDevice = false;
foreach (e : acl) {
    if (e.entityType == "DEVICE" && e.entityId == msg.targetDeviceId
        && e.allowed.contains("WRITE")) {
        canWriteTargetDevice = true;
        break;
    }
}
metadata.auditActor = metadata.tb_userId;
return {msg: msg, metadata: metadata, msgType: canWriteTargetDevice ? "Allowed" : "Denied"};
```

### Filter pattern (entities the caller can WRITE)

```js
var writable = JSON.parse(metadata.tb_aclSnapshot)
    .filter(function(e) { return e.allowed.indexOf('WRITE') >= 0; })
    .map(function(e) { return e.entityType + ':' + e.entityId; });
```

## 10. Error Handling

| Condition | Response |
|---|---|
| Malformed JSON body | `400 Bad Request` (existing behavior) |
| `enrichEntities.size() > maxAclEntities` | `400 Bad Request` with message |
| Unknown `entityType` (`EntityIdDeserializer` fails) | `400 Bad Request` (existing behavior) |
| No WRITE on originator | `401 Unauthorized` (existing `AccessValidator`) |
| `EntityType` has no matching `Resource` | Entry with `allowed=[]`; WARN log; no HTTP error |
| `EntityType` has no `EntityDaoService` registered | Entry with `allowed=[]`; WARN log; no HTTP error |
| Entity not found / cross-tenant / not `HasTenantId` | Entry with `allowed=[]`; WARN log; no HTTP error |
| Operation inapplicable to role (throws) | Silently skipped from `allowed` — no error |
| Null/missing `payload` | Treated as empty JSON object `{}` — no error |
| Rule Engine processing timeout | `408 Request Timeout` (existing) |

## 11. Security Considerations

- **Immutable server fields.** `tb_aclSnapshot` and `tb_userId` are written by the controller after all other metadata population, so any caller-supplied value of the same key is overwritten. `TbMsgMetaData.putValue(...)` is replace semantics — no array/append path.
- **No privilege escalation.** The ACL snapshot reports *only* what the user already has on each specific entity. It can never grant more than the user can do directly.
- **Information disclosure.** The snapshot tells the rule chain (which the user cannot read directly) what the caller is allowed to do on each listed entity. Since the caller supplied the list and the data is about themselves, this is not a new information leak. The response shape for an entity the caller cannot READ is **indistinguishable** from the shape for a nonexistent entity — both produce `{entityType, entityId, allowed: []}` — so the endpoint cannot be used as an existence probe for tenant-scoped entities outside the caller's access scope.
- **DoS vector.** Bounded by:
  - up to `max-entities` entity loads from the DAO (≤20 SELECT statements per request — many DAOs have L2 cache, so repeated lookups within a session are essentially free);
  - `max-entities × Operation.values().length` in-memory permission checks (≤20 × 18 = 360 checks worst case).
  Dedup cache (§6) collapses repeated ids in a single request to one load + one check set.
- **Audit.** `tb_userId` inside rule chains lets authors emit richer audit events that today's `REST_API_RULE_ENGINE_CALL` audit log does not propagate.

## 12. Test Plan

### Integration (controller)

All tests in `RuleEngineControllerV2Test`, `@DaoSqlTest`, extending `AbstractControllerTest`, with `@SpyBean RuleEngineCallService` to capture the forwarded `TbMsg`.

- Tenant admin + own DEVICE → `allowed` contains at least READ/WRITE/DELETE/WRITE_TELEMETRY; `tb_userId` equals caller UUID.
- Customer user + DEVICE assigned to the user's customer → `allowed` contains the customer-grade operations (WRITE, READ_TELEMETRY, etc.).
- Customer user + DEVICE assigned to a DIFFERENT customer → `allowed` does not contain WRITE/READ/READ_TELEMETRY (per-entity check correctly denies). Note: the platform allows `CLAIM_DEVICES` on any tenant device by design — that is expected to be present.
- Two-customer cross-scenario: deviceA on customer1, deviceB on customer2; customer1 user sees WRITE on A and not on B; customer2 user sees the inverse.
- Empty or null `enrichEntities` → `tb_aclSnapshot = "[]"`; `tb_userId` still present.
- Duplicate ids in `enrichEntities` → output preserves order and multiplicity.
- `enrichEntities.size() > max` → HTTP 400.
- `RULE_NODE` (no `Resource` mapping) → entry with `allowed=[]`, no error.
- Nonexistent UUID for a valid `EntityType` → entry with `allowed=[]`, no error.
- Caller embeds `{"tb_aclSnapshot": "attack", "tb_userId": "intruder"}` in `payload` → forwarded metadata keeps server-computed values; the caller's keys never reach the rule engine.
- Body `timeout=...` → forwarded `expirationTime` reflects the body value; `TbMsg.type` is always `REST_API_REQUEST` (matches v1).
- Null/missing `payload` → forwarded `TbMsg.data` is `"{}"`.
- v1 endpoints regression suite (`RuleEngineControllerTest`) continues to pass.

## 13. Out of Scope / Future Work

- **Extending enrichment to other controllers that push to the Rule Engine** (e.g., `TelemetryController`, `RpcController`). They have the same "message in RE has no user context" gap. Each would be its own additive v2 endpoint. If/when we need this in more than one controller, it would also motivate extracting `buildAclMetadata` / `computeEntry` into a shared `AclEnrichmentService` (Variant B from the original design — deferred until a real second caller exists).
- **Additional enriched fields** (e.g., `tb_user_email`, `tb_user_authority`) if rule-chain authors ask for them. Each is a small additive change. We avoid speculative addition — extra fields mean larger metadata and more invariants to keep stable across renames.
- **Batched / parallel entity load.** `buildAclMetadata` currently calls `entityService.fetchEntity(...)` sequentially on the validation-callback thread. With `max-entities=20` the worst case is 20 sequential DAO reads before the message is forwarded; many entity DAOs use L2 cache so warm-path cost is much lower. A batch API returning the `TenantEntity`-typed objects required by the per-entity `hasPermission` form (today's `fetchEntityInfos` returns `EntityInfo` only, which is insufficient) would let us collapse this to a single round trip. Deferred until the bound becomes a measured problem.

## 14. Open Questions

None at approval time.

---

## 15. Manual Smoke-Test Recipe

1. Start ThingsBoard locally (Postgres + core + rule-engine node).
2. Log in as Tenant Admin. Create a simple rule chain:
   - **Script** node reading `metadata.tb_aclSnapshot` and setting `msgType` to `Allowed` or `Denied`.
   - **REST Call Reply** node.
3. Set this rule chain as root (or route via `queueName`).
4. Call:

   ```bash
   curl -X POST http://localhost:8080/api/rule-engine/v2 \
     -H "Content-Type: application/json" \
     -H "X-Authorization: Bearer $JWT" \
     -d '{
       "payload": {"k":"v"},
       "enrichEntities": [
         {"entityType":"DEVICE","id":"<some-device-uuid>"}
       ]
     }'
   ```

5. Expected: HTTP 200 with body `{"msgType":"Allowed"}` (tenant admin) or `{"msgType":"Denied"}` (customer user without per-entity access).
6. In Rule Chain debug mode, inspect the incoming `TbMsg` — `metadata.tb_aclSnapshot` and `metadata.tb_userId` are present and authoritative.
