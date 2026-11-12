# Edge tenant isolation: fixes summary

Confidential: PROD-9075, PROD-9081. Keep out of public PRs, commits and changelogs.

**Common cause:** messages from an edge were not consistently checked against the edge's own tenant. The cloud took the tenant id from the payload, looked entities up by id without a tenant filter, and didn't validate referenced ids.

**Merge order:** CE lts-4.2 → PE lts-4.2 → CE lts-4.3 → PE lts-4.3 (merges CE 4.3 and PE 4.2) → CE lts-4.4. CE `lts-4.4` merges CE `lts-4.3` (CE history only); PE-only fixes reach CE 4.4 as separate hand-written CE commits, never through a PE merge. Fixes in 4.2 reach later branches through the regular merges. Later sections list only what is new on that branch.

---

## PR matrix

Merge in this order. Each PR already contains the previous one in its chain, and every PR and forward merge (CE 4.2 → CE 4.3 → CE 4.4, CE 4.2 → PE 4.2 → PE 4.3, CE 4.3 → PE 4.3) was checked to merge without conflicts against upstream as of 2026-10-27.

| # | Repo | Version | PR | Branch → base | Head | Contains | After merge |
|---|---|---|---|---|---|---|---|
| 1 | CE | 4.2 | [#16451](https://github.com/thingsboard/thingsboard/pull/16451) | `fix/edge-uplink-msgs-processing-lts-4.2` → `lts-4.2` | `728e0016ad3` | CE 4.2 fixes | Merge CE `lts-4.2` into PE `lts-4.2` and CE `lts-4.3` |
| 2 | PE | 4.2 | [#5279](https://github.com/thingsboard/thingsboard-pe/pull/5279) | `pe-fix/edge-uplink-msgs-processing-lts-4.2` → `lts-4.2` | `4b08f3bdec3` | #1 + PE group/owner checks | Merge PE `lts-4.2` into PE `lts-4.3` |
| 3 | CE | 4.3 | [#16452](https://github.com/thingsboard/thingsboard/pull/16452) | `fix/edge-uplink-msgs-processing-lts-4.3` → `lts-4.3` | `92140de0792` | #1 + user, API key, AI model, delete paths | Merge CE `lts-4.3` into PE `lts-4.3` and CE `lts-4.4` |
| 4 | PE | 4.3 | [#5280](https://github.com/thingsboard/thingsboard-pe/pull/5280) | `pe-fix/edge-uplink-msgs-processing-lts-4.3` → `lts-4.3` | `15125d8909d` | #2 + #3 + user group removal, report templates, scheduler events | — (4.4 is CE only; never merge PE into CE) |
| 5 | CE | 4.4 | [#16453](https://github.com/thingsboard/thingsboard/pull/16453) | `fix/edge-uplink-msgs-processing-lts-4.4` → `lts-4.4` | `dcb9c6dada2` | #3 + group/owner, user group removal, report templates, scheduler events (written as CE commits) | — |

---

## CE lts-4.2

Branch `fix/edge-uplink-msgs-processing-lts-4.2`. Edge test suite: 121/121 pass, and each new test fails without its fix.

| # | Area | Processors / paths | Issue | Fix | Tests | Status |
|---|---|---|---|---|---|---|
| 1 | Request messages | `DefaultEdgeRequestsService`: user credentials, device credentials, attributes, relations, rule chain metadata, widget bundle types | Requested ids were not checked against the edge's tenant, so the response could contain another tenant's data | `isTenantEntity` check before queuing the response. Widget bundles: own tenant or system tenant only | `UserEdgeTest`, `DeviceEdgeTest`, `RelationEdgeTest`, `RuleChainEdgeTest` (`…RequestFor…OfDifferentTenant…`) | Fixed |
| 2 | Entities with a tenant | device, device profile, asset, asset profile, dashboard, entity view, resource, calculated field, rule chain | Tenant taken from the payload; an id from another tenant could be created or overwritten | `checkEntityTenant` rejects foreign ids; tenant forced; validator runs against the edge's tenant; rule chain id taken from the message | `DeviceEdgeTest`, `DashboardEdgeTest`, `ResourceEdgeTest` (`…OfDifferentTenant…`, `…WithSystemTenantId…`) | Fixed |
| 3 | References | entity view target; calculated field entity and arguments; rule chain metadata target | Referenced entities were not checked against the edge's tenant | Must belong to the edge's tenant (`isEntityExists`) | `EntityViewEdgeTest`, `CalculatedFieldEdgeTest`, `RuleChainEdgeTest` | Fixed |
| 4 | Telemetry | attributes update/post, telemetry post, attribute delete | Target entity was only checked to exist | Target must belong to the edge's tenant (shared `isEntityExists`) | `TelemetryEdgeTest` | Fixed |
| 5 | Alarms | alarm, alarm comment | Originator and tenant taken from the payload; comments matched by id only | Originator in the edge's tenant; tenant forced; alarm id checked; comment must belong to an alarm of the edge's tenant | `AlarmEdgeTest` | Fixed |
| 6 | Relations | create, delete | Delete had no tenant check; create used an unscoped check | Both ends must belong to the edge's tenant | `RelationEdgeTest` | Fixed |
| 7 | Device RPC | status, response, request | RPC/device ids not checked | RPC and device must belong to the edge's tenant | covered by the existing RPC tests | Fixed |
| 8 | Unassign on delete | asset, dashboard, entity view, rule chain, calculated field, device (device: explicit guard added for consistency; the tenant-scoped `findDeviceById` already returns null for a foreign device) | Entity tenant not checked | Tenant guard | covered by the existing delete tests | Fixed |
| 9 | System-context events | `BaseEdgeProcessor.processActionForAllEdges`, `EdgeEventType`, `UserEdgeProcessor` | Events about tenant users raised without a tenant (auth flows, sysadmin actions) were queued for every active edge | `EdgeEventType.systemLevelSupported` (11 system-ownable types) and `allEdgesRelated` renamed to `allTenantEdgesRelated`. In system context: system-level types are broadcast, other types go to the entity's own tenant or are skipped. A user is sent only to edges of its own tenant | `UserEdgeTest`: `testUserActivationInDifferentTenant`, `testSysAdminCredentialsUpdate`, `testTenantUserCredentialsUpdate` | Fixed |
| 9a | System-context events, edge-assigned types | `BaseEdgeProcessor.processEntityNotification` (edge-assigned branch) | Where users are edge-assigned (PE, CE 4.4), credentials updated in system context (activation, password reset completion) were saved under the system tenant and never delivered to the user's edges | The notification is re-processed under the entity's own tenant; skipped when the entity has no tenant | `UserEdgeTest.testTenantUserCredentialsUpdate` (fails on plain PE 4.2) | Fixed |
| 10 | Email / SMS / notifications | `processSendEmailMsg`, `processSendSmsMsg`, `processSendNotificationMsg` | Recipients and content aren't restricted | Left to the planned "smart send" work; tenant-profile limits apply on PaaS | — | Not changed |
| 11 | Alarm comment author | alarm comment | Author user id taken from the payload | No cross-tenant write; left as is | — | Not changed |
| 12 | Rule node configurations | rule chain metadata | Node configs saved as sent | They run in the edge's own tenant; left as is | — | Not changed |
| 13 | Widget bundle request | `processWidgetBundleTypesRequestMsg` | — | Fixed in #1 | No dedicated test | Test gap |

---

## PE lts-4.2 (after merging CE 4.2)

Branch `pe-fix/edge-uplink-msgs-processing-lts-4.2`. Edge suite after the merge: 140/141; the only failure (`testTenantUserCredentialsUpdate`) also failed on plain PE 4.2 and is fixed by CE 4.2 #9a.

| # | Area | Processors / paths | Issue | Fix | Tests | Status |
|---|---|---|---|---|---|---|
| 1 | Entity groups | add to group (`safeAddEntityToGroup`); remove from group on delete (device, asset, dashboard, entity view) | Group id from the edge not checked | Group owner (tenant or its customer) must belong to the edge's tenant (`safeRemoveEntityFromGroup`) | `EntityGroupEdgeTest.testSendDeviceDeleteFromGroupOfDifferentTenantToCloud` | Fixed |
| 2 | Owner change | `changeOwnerIfRequired` | Target customer taken from the payload | Rejected unless the owner belongs to the edge's tenant | `DeviceEdgeTest.testSendDeviceUpdateWithCustomerOfDifferentTenantToCloud` | Fixed |
| 3 | Entity group entities request | `processEntityGroupEntitiesRequest` | Group looked up without tenant check (members already re-filtered) | Skipped unless the group owner belongs to the edge's tenant | `EntityGroupEdgeTest.testSendEntityGroupEntitiesRequestForGroupOfDifferentTenantToCloud` (regression only) | Fixed |
| 4 | Scheduler events, report templates | — | No uplink processors in PE 4.2 (they exist on PE 4.3, see CE 4.4 #6–#7) | — | — | N/A |

---

## CE lts-4.3 (after merging 4.2)

| # | Area | Processors / paths | Issue | Planned fix | Status |
|---|---|---|---|---|---|
| 1 | User uplink | `BaseUserProcessor`: user save/update/delete, credentials | Tenant and authority taken from the payload; user ids from other tenants accepted | Tenant forced; authorities limited to tenant admin and customer user; cross-tenant user ids rejected | Fixed (#16432/#16433) |
| 2 | User credentials id | `BaseUserProcessor.updateUserCredentials` | Credentials record id from the payload is not checked | New `UserService.findUserCredentialsById`; skipped if the record belongs to a different user. Test: `UserEdgeTest.testSendUserCredentialsWithIdOfDifferentTenantUserToCloud` | Fixed |
| 3 | Authority test | `UserEdgeTest` | Test covers only SYS_ADMIN, which the validator also rejects | `testSendUserWithTokenAuthorityFromEdgeToCloud` (REFRESH_TOKEN) | Fixed |
| 4 | API keys | `BaseApiKeyProcessor` | Tenant and user taken from the payload; lookup by id unscoped | Foreign id rejected; tenant forced; user must be in the edge's tenant; tenant guard on delete. `internal`/`permissions` don't exist on 4.3 (see 4.4). Test: `ApiKeyEdgeTest.testSendApiKeyForUserOfDifferentTenantToCloud` | Fixed |
| 5 | AI models | `BaseAiModelProcessor` | Tenant taken from the payload; lookup by id unscoped | Foreign id rejected; tenant forced. Test: `AiModelEdgeTest.testSendAiModelWithTenantIdOfDifferentTenantToCloud` | Fixed |
| 6 | User downlink with API keys | `UserEdgeProcessor` ADDED/UPDATED | Also carries API keys | Covered by CE 4.2 #9; verify after merge | Verify |
| 7 | Edge credentials visibility | `EdgeController.getEdgeById` | Edge credentials visible to customer users with access to the edge | Product decision | Open |
| 8 | New event types | `EdgeEventType` entries added in 4.3 | — | `AI_MODEL`, `API_KEY`: not system-level (system-owned internal keys are skipped) | Fixed |
| 9 | Delete paths | asset, dashboard, entity view deletes really delete on 4.3 | Tenant guard inside `deleteAsset`, `deleteDashboard`, `deleteEntityView` | Fixed |
| 10 | Device delete | `BaseDeviceProcessor.deleteDevice` | Looked like an unguarded real delete | Not a gap: `findDeviceById` with a tenant id uses `findDeviceByTenantIdAndId`, so a foreign device is not found. Regression test `DeviceEdgeTest.testSendDeviceDeleteForDeviceOfDifferentTenantToCloud` (from 4.2) | OK |

---

## PE lts-4.3 (after merging CE 4.3)

| # | Area | Planned fix | Status |
|---|---|---|---|
| 1 | PE-only items from PE 4.2 | Merged PE 4.2 (`077ea1f16e2`); group removal on delete uses `safeRemoveEntityFromGroup`, while the edge ≥ 4.3.0 branch still deletes | Done |
| 2 | CE 4.3 items on PE code paths | Merged CE 4.3 (`6b66b550fca`); `REPORT_TEMPLATE` is not system-level (tenant/customer owned only) | Done |
| 3 | Device delete | Not a gap (see CE 4.3 #10); merged CE 4.3 `73f1933c680` and PE 4.2 `10ce7c7a3d7`, pushed to nm-pe | Done |

---

## CE lts-4.4 (after merging CE 4.3)

Branch `fix/edge-uplink-msgs-processing-lts-4.4` from `tb-ce/lts-4.4`. CE 4.3 merged as usual (CE history only). PE-only changes are written by hand as a new CE commit; nothing is merged or cherry-picked from PE.

| # | Area | Issue | Fix | Tests | Status |
|---|---|---|---|---|---|
| 0 | API keys | `internal`/`permissions` exist on 4.4 | No change needed: `ApiKeyServiceImpl.saveApiKey` clears both for keys not owned by the system tenant, and the tenant is forced; system-tenant (internal) keys are rejected by `checkEntityTenant` and skipped on delete | `ApiKeyEdgeTest.testSendInternalApiKeyWithPermissionsToCloud` | OK |
| 1 | Delete paths | Edge deletes really delete | Asset, dashboard, entity view, API key, calculated field, user: guarded. Device: tenant-scoped lookup. AI model: `deleteByTenantIdAndId` | — | OK |
| 2 | Tests | Message constructors take an extra entity group argument | `null` passed in the new tests | — | Done |
| 3 | New event types | — | None new on 4.4; white labeling, login white labeling, mail templates, custom translation and custom menu are system-level | — | Done |
| 4 | Entity groups / owner (PE features on 4.4) | Same as PE 4.2 #1–#3 | `safeAddEntityToGroup` and `safeRemoveEntityFromGroup` check the group owner; `changeOwnerIfRequired` rejects foreign owners; group entities request checks the group owner | `EntityGroupEdgeTest` (device and user group removal, group entities request), `DeviceEdgeTest.testSendDeviceUpdateWithCustomerOfDifferentTenantToCloud` | Fixed |
| 5 | User removal from group | `UserEdgeProcessor` removed users from any group by id (also on PE 4.3) | `safeRemoveEntityFromGroup` | `EntityGroupEdgeTest.testSendUserDeleteFromGroupOfDifferentTenantToCloud` | Fixed (4.4; PE 4.3 `95d3e3aed0e`, `bbd0eff9832`) |
| 6 | Report templates | Uplink processor (also on PE 4.3): tenant from payload, lookup by id only, delete unguarded | `checkEntityTenant`, tenant forced, delete guard; customer is checked by the validator once the tenant is forced | `ReportTemplateEdgeTest` (`…WithTenantIdOfDifferentTenant…`, `…UpdateAndDeleteOfDifferentTenant…`) | Fixed (4.4; PE 4.3 `95d3e3aed0e`, `bbd0eff9832`) |
| 7 | Scheduler events | Uplink processor (also on PE 4.3): tenant from payload, lookup by id only; `GENERATE_REPORT` config could name another tenant's report template or user (the controller checks both) | `checkEntityTenant`, tenant forced, report template and user must belong to the edge's tenant; unassign guarded | `SchedulerEventEdgeTest` (`…WithTenantIdOfDifferentTenant…`, `…GenerateReport…WithUserOfDifferentTenant…`) | Fixed (4.4; PE 4.3 `95d3e3aed0e`, `bbd0eff9832`) |
| 8 | Build | lts-4.4 compiled locally this time | — | — | Note |

---

## Follow-up: external user uplink review (2026-10-28)

The review read upstream CE `lts-4.3` / `lts-4.4` before our PRs and didn't know about the PE repo. Status against our branches:

| Review # | Finding | Status |
|---|---|---|
| 1 | Edge creates a system admin / admin in another tenant | Fixed upstream (`98c3aad861a`) |
| 2 | Credentials record id of another user reused | Fixed (CE 4.3 #2) |
| 2 | Credentials overwrite across tenants | Fixed upstream |
| 3 | Edge requests any user's credentials | Fixed (CE 4.2 #1, `isTenantEntity` in `processUserCredentialsRequestMsg`) |
| 4, 5 | Delete / edit users across tenants | Fixed upstream |
| A, B | Owner change / group membership across tenants (4.4, PE 4.3) | Fixed (CE 4.4 #4–#5, PE 4.3 `95d3e3aed0e`) |

Remaining items, in fix order (customer-level items are deferred, see below). All are pushed and merged forward: PE 4.2 `a900b576169`, PE 4.3 `864f0d82aa1`, CE 4.4 `78541941f9f` (on CE 4.4, #1 re-applied by hand in `rpc/session/EdgeGrpcSession` and `EdgeUplinkMessageDispatcher`, which replace the deleted `rpc/EdgeGrpcSession`):

| # | Branch | Issue | Review # | Decision | Status |
|---|---|---|---|---|---|
| 1 | CE 4.2 | Edge secret, msg bodies and credentials in edge failure notifications and logs | 7 | Replace msg bodies with type/id/count summaries (`EdgeMsgSummaryUtils`); drop the secret; redact device credentials logs. TRACE logs and non-credential entity JSON in processor logs left as is | Pushed `46ea9dbd883`; `DeviceEdgeTest`, `UserEdgeTest` pass |
| 2 | CE 4.2 | Activation/reset tokens sent to edges with user credentials | 8 | Strip `activateToken`, `resetToken` and their expiry from the credentials downlink; keep the hash (needed for local login). Shared constructor, so it covers PE and 4.3+ after merges | Pushed `728e0016ad3` |
| 2a | CE 4.2 | `additionalInfo` (password history hashes) still sent to edges | 8 | Leave as is: the edge already has the current hash, old hashes add little and the edge uses them for its password-reuse check. The credentials uplink must not overwrite the cloud's history (see #3) | Won't fix |
| 3 | CE 4.3 | Credentials uplink accepts reset/activation tokens, `failedLoginAttempts`, `additionalInfo`; saved with validation off | 2 | Merge instead of replace: only `password` and `enabled` from the edge, the rest stays as on cloud; password history updated on change (`UserService.updatePasswordHistory` made public). Activation: edge `enabled=true` clears the cloud's activation token; edge pending activation (not enabled, has token) makes the cloud generate its own token if it has none. Also restored the `@Test` on `UserEdgeTest.testSendUserCredentialsRequestForUserOfDifferentTenantToCloud`, lost in the CE 4.2 → 4.3 merge (missing on PE 4.3 and CE 4.4 too) | Pushed `6af0dd38ce3`, `2b7830a03fc`; `UserEdgeTest` 15/15, new test fails without the fix |
| 4 | CE 4.3 | User, user credentials, API key and AI model msgs in logs and in exception texts (which reach the failure notification) | 7 | Same approach as #1: ids (user, API key, AI model) or `EdgeMsgSummaryUtils` summaries instead of msg bodies; no msg content in exceptions; TRACE left as is | Pushed `02e854f79b7`; `UserEdgeTest`, `ApiKeyEdgeTest`, `AiModelEdgeTest` pass |
| 5 | CE 4.3 | Duplicate email in another tenant blocks the edge's user uplink and reveals that the email exists | 9 | Check the email across the platform (`findUserByEmail`, same case handling as `saveUser`) and rename as for same-tenant duplicates. Same on Citus: `tb_user` is a reference table, uniqueness enforced cluster-wide. The "email exists" hint isn't new (user creation via REST gives the same error) | Pushed `0475763bba7`; `UserEdgeTest` 16/16, new test fails without the fix |

### Deferred: customer-level isolation

Tracked in [EDGE-122](https://thingsboard-portal.atlassian.net/browse/EDGE-122) (work scope, decisions and known gaps); linked to PROD-9081.

All of these stay inside the edge's own tenant: a customer-owned/assigned edge acting on tenant-level or other customers' data. Put aside on 2026-10-29.

**Customer-level isolation must be addressed separately**, as its own task and PR chain, not as part of this tenant isolation work:

- **Different scope.** PROD-9075/9081 and these PRs cover tenant isolation: an edge of tenant A can't read or change tenant B's data. Customer-level isolation is the next level down: an edge assigned to (CE) or owned by (PE) customer X must not change tenant-level data (tenant admins, tenant groups) or data of customer Y in the same tenant.
- **Why it matters.** A customer edge often runs at the customer's site. A stolen edge or extracted credentials currently allow creating a `TENANT_ADMIN`, setting a tenant admin's password, editing or deleting other customers' users, adding a user to the Tenant Administrators group (PE), and offline cracking of tenant admin hashes. A compromise at one customer site can escalate to the whole tenant.
- **Needs product decisions first.** It is not a missing check but an undefined access model for customer edges: whether tenant admins may log in locally on customer edges (if not, their hashes stop being sent, which changes current behaviour), whether a customer edge may manage only its own customer's users and only with customer authorities, and whether PE should stop auto-assigning tenant groups to customer-owned edges.
- **Starting point.** The table below and the review leftovers list every known customer-level gap with the options discussed so far.

| Branch | Issue | Review # | Notes |
|---|---|---|---|
| CE 4.2 | Every edge, including customer-assigned ones, gets all tenant admins with password hashes (`TenantAdminUsersEdgeEventFetcher`) | 8 | Option discussed: don't send tenant admin hashes to customer edges (breaks local login for tenant admins there) |
| PE 4.2 | `DefaultTbEdgeService` auto-assigns Tenant Administrators and Tenant Users groups to every new edge, customer-owned too; moving an edge to a customer keeps them. Customer edges get tenant admin hashes by default | 8 | Configurable in PE (groups can be unassigned). Option discussed: stop auto-assigning tenant groups to customer-owned edges |
| CE 4.3 | Customer edge can create `TENANT_ADMIN` users, and edit, delete or set passwords of tenant admins and other customers' users; customer id taken from the msg (`setCustomerId`) | 2, 4, 5, 6 | Needs "edge customer" checks on create/update/delete/credentials |
| PE 4.3 / CE 4.4 | Customer edge can move users between customers: `changeOwnerIfRequired` runs before `userValidator.validate` | A | Cross-tenant part fixed |
| PE 4.3 / CE 4.4 | Customer edge can add a user to any group of its tenant (e.g. Tenant Administrators); `addEntityToEntityGroup` doesn't check the owner | B | Cross-tenant part fixed; group membership carries permissions in PE |

### Leftovers from the review (after all PRs)

Everything from the review that is not fully closed on our branches, by review finding:

| Review # | What is left | Why / decision | Status |
|---|---|---|---|
| 2 | A customer edge can set a tenant admin's (or another customer's user's) password | Customer-level isolation | Deferred: EDGE-122 |
| 4 | A customer edge can delete tenant admins and other customers' users; on 4.4 / PE 4.3 a delete with an entity group id removes the user from a group of its tenant | Customer-level isolation | Deferred: EDGE-122 |
| 5 | A customer edge can edit tenant admins, including email, then reset their password | Customer-level isolation | Deferred: EDGE-122 |
| 6 | Customer id taken from the msg (`setCustomerId`); a customer edge can create `TENANT_ADMIN` users or users under any customer of the tenant | Customer-level isolation | Deferred: EDGE-122 |
| 7 | TRACE logs still print whole msgs | Developer-only, off in production | Left as is |
| 7 | Non-credential entity JSON (device, asset, dashboard, entity view, relation, rule chain, profiles, calculated field, scheduler event, report template) still in processor ERROR/WARN logs and in "cannot be converted" exception texts, which reach the edge failure notification | No credentials or secrets; same tenant's data | Left as is |
| 8 | Password hashes are still sent to edges (local login needs them); every CE edge gets all tenant admins, PE auto-assigns tenant groups to customer-owned edges | Hash itself is by design; the customer-edge part is customer-level isolation | Deferred: EDGE-122 |
| 8 | Password history (`additionalInfo.userPasswordHistory`) sent to edges | Edge already has the current hash; edge uses the history for its reuse check | Won't fix |
| A | On 4.4 / PE 4.3 `changeOwnerIfRequired` runs before `userValidator.validate`, so a customer edge can move a user to another customer of its tenant | Cross-tenant owners are rejected; customer-level isolation | Deferred: EDGE-122 |
| B | On 4.4 / PE 4.3 a customer edge can add a user to any group of its tenant (e.g. Tenant Administrators); group membership carries permissions in PE | Cross-tenant groups are rejected; customer-level isolation | Deferred: EDGE-122 |
| — | If the edge builds its credentials uplink with the shared `constructUserCredentialsUpdatedMsg`, it no longer sends the activation token, so a user created on the edge and not yet activated shows as activated on the cloud | Display only; needs a check in the edge repo | Open question |
| Other | Upstream `lts-4.4` has 3,444 Java files under BUSL-1.1 (`upstream` = public `thingsboard/thingsboard`), `lts-4.3` has none | Not an edge issue; for product/legal to confirm | Not ours |

---

## PaaS
Same code as PE; follows the PE merges.
