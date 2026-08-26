/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.model.sql.RpcEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JpaRpcDaoTest extends AbstractJpaDaoTest {

    @Autowired
    JpaRpcDao rpcDao;

    @Autowired
    RpcWriteRepository rpcWriteRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private UUID rawSeededId;

    @After
    public void cleanupRawSeededRow() {
        if (rawSeededId != null) {
            jdbcTemplate.update("DELETE FROM rpc WHERE id = ?", rawSeededId);
            rawSeededId = null;
        }
    }

    @Test
    public void deleteOutdated() throws Exception {
        // Dedicated tenants, NOT SYS_TENANT_ID: the Long.MAX_VALUE case deletes every row of the tenant it is
        // given, and this class has no per-test rollback, so sharing a tenant with the other tests would make
        // the exact counts below depend on JUnit's method ordering.
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TenantId otherTenantId = TenantId.fromUUID(UUID.randomUUID());
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        create(rpc(tenantId, UUID.randomUUID(), deviceId, RpcStatus.QUEUED, null));
        create(rpc(tenantId, UUID.randomUUID(), deviceId, RpcStatus.QUEUED, null));
        create(rpc(otherTenantId, UUID.randomUUID(), new DeviceId(UUID.randomUUID()), RpcStatus.QUEUED, null));

        int batchSize = 10_000;
        assertThat(rpcDao.deleteOutdatedRpcByTenantIdBatch(tenantId, 0L, batchSize)).isEqualTo(0);
        assertThat(rpcDao.deleteOutdatedRpcByTenantIdBatch(tenantId, Long.MAX_VALUE, batchSize)).isEqualTo(2);
        assertThat(rpcDao.deleteOutdatedRpcByTenantIdBatch(otherTenantId, System.currentTimeMillis() + 1, batchSize)).isEqualTo(1);
    }

    @Test
    public void syncCreateThenAsyncUpdateConvergesToUpdatedStatus() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        create(rpc(id, deviceId, RpcStatus.QUEUED, null));

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.QUEUED);
        assertThat(stored.getResponse()).isNull();

        // The async status update matches the existing row, so the future resolves true.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.DELIVERED, JacksonUtil.toJsonNode("{\"ok\":true}")))
                .get(5, TimeUnit.SECONDS)).isTrue();

        Rpc afterUpdate = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(afterUpdate.getStatus()).isEqualTo(RpcStatus.DELIVERED);
        assertThat(afterUpdate.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
    }

    @Test
    public void updateBatchAppliesInOrderAndAlignsResults() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID(); // never created -> its update must report no match

        create(rpc(idA, deviceId, RpcStatus.QUEUED, null));

        // Drive the persist logic directly with a single, deterministically-coalesced update batch.
        // This is exactly what "coalescing" means: one update() call carrying several writes for the
        // same partition in submission order. No queue timing involved.
        //   index 0: update A (SUCCESSFUL, {ok:true}) -> UPDATE hits the existing row  -> true
        //   index 1: update B (SUCCESSFUL, {x:1})     -> UPDATE for a missing row      -> false
        //   index 2: update A (EXPIRED) -> guard blocks overwrite of terminal SUCCESSFUL -> false
        List<RpcWrite> batch = List.of(
                RpcWrite.update(new RpcEntity(rpc(idA, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"ok\":true}")))),
                RpcWrite.update(new RpcEntity(rpc(idB, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"x\":1}")))),
                RpcWrite.update(new RpcEntity(rpc(idA, deviceId, RpcStatus.EXPIRED, null))));

        List<Boolean> persisted = rpcWriteRepository.write(batch);

        // Booleans align positionally to submission order:
        //   index 0: A QUEUED -> SUCCESSFUL      -> allowed          -> true
        //   index 1: B (missing row)             -> no match         -> false
        //   index 2: A SUCCESSFUL -> EXPIRED     -> guard blocks it  -> false  (terminal is immutable)
        assertThat(persisted).containsExactly(true, false, false);

        // A stays SUCCESSFUL with its response: the in-batch EXPIRED was rejected by the guard.
        Rpc storedA = rpcDao.findById(TenantId.SYS_TENANT_ID, idA);
        assertThat(storedA).isNotNull();
        assertThat(storedA.getStatus()).isEqualTo(RpcStatus.SUCCESSFUL);
        assertThat(storedA.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
        // ...and the update for a never-created row neither persisted nor resurrected it.
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, idB)).isNull();
    }

    @Test
    public void saveAsyncRequeueUpdatesExistingRowToQueued() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        // Initial create.
        create(rpc(id, deviceId, RpcStatus.QUEUED, null));

        // Delivery timeout with closeTransportSessionOnRpcDeliveryTimeout=true re-queues the RPC: the
        // device actor persists status=QUEUED again as a status update so init() can re-pick it up.
        // The update must land on the existing row, never be dropped.
        rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.QUEUED, null)).get(5, TimeUnit.SECONDS);

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.QUEUED);
    }

    @Test
    public void guardBlocksExpiredOverwritingSuccessful() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        create(rpc(id, deviceId, RpcStatus.QUEUED, null));

        // A real device response reaches SUCCESSFUL with a response body.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"ok\":true}")))
                .get(5, TimeUnit.SECONDS)).isTrue();

        // A stale EXPIRED (e.g. from a migrated actor's timeout) must NOT overwrite it: future resolves false.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.EXPIRED, null)).get(5, TimeUnit.SECONDS)).isFalse();

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.SUCCESSFUL);
        assertThat(stored.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
    }

    @Test
    public void guardBlocksExpiredOverwritingOneWayDelivered() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Rpc created = rpc(id, deviceId, RpcStatus.DELIVERED, null);
        created.setOneway(true);
        create(created);

        // One-way DELIVERED is terminal. A one-way write carries oneway=true (as the actor sets from the request),
        // so its allowed-from set excludes DELIVERED and the guard blocks the overwrite.
        Rpc staleExpired = rpc(id, deviceId, RpcStatus.EXPIRED, null);
        staleExpired.setOneway(true);
        assertThat(rpcDao.updateAsync(staleExpired).get(5, TimeUnit.SECONDS)).isFalse();
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id).getStatus()).isEqualTo(RpcStatus.DELIVERED);
    }

    @Test
    public void guardAllowsExpiredOverwritingTwoWayDelivered() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Rpc created = rpc(id, deviceId, RpcStatus.DELIVERED, null);
        created.setOneway(false);
        create(created);

        // Two-way DELIVERED is in-flight (awaiting response), so a genuine timeout may expire it.
        Rpc expired = rpc(id, deviceId, RpcStatus.EXPIRED, null);
        expired.setOneway(false);
        assertThat(rpcDao.updateAsync(expired).get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id).getStatus()).isEqualTo(RpcStatus.EXPIRED);
    }

    @Test
    public void guardAllowsSuccessfulFromSent() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        create(rpc(id, deviceId, RpcStatus.SENT, null));

        // Response for an as-yet-undelivered RPC (no PUBACK) lands while status is still SENT.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"v\":1}")))
                .get(5, TimeUnit.SECONDS)).isTrue();
        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.SUCCESSFUL);
        assertThat(stored.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"v\":1}"));
    }

    @Test
    public void guardBlocksSentDowngradingDelivered() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Rpc created = rpc(id, deviceId, RpcStatus.DELIVERED, null);
        created.setOneway(false);
        create(created);

        // A stale/duplicate SENT must not roll DELIVERED backwards.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SENT, null)).get(5, TimeUnit.SECONDS)).isFalse();
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id).getStatus()).isEqualTo(RpcStatus.DELIVERED);
    }

    @Test
    public void saveAsyncUpdateForDeletedRpcDoesNotResurrect() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        create(rpc(id, deviceId, RpcStatus.QUEUED, null));

        // RPC is removed (TTL cleanup / manual delete) while a response is still in flight.
        rpcDao.removeById(TenantId.SYS_TENANT_ID, id);
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id)).isNull();

        // A late status update must not re-create the deleted row, and the future must resolve false
        // (no row matched) so the service layer can skip the rule-engine notification.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"ok\":true}")))
                .get(5, TimeUnit.SECONDS)).isFalse();

        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id)).isNull();
    }

    @Test
    public void createIfAbsentInsertsWhenRowMissing() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Rpc toCreate = rpc(id, deviceId, RpcStatus.QUEUED, null);
        toCreate.setRequestId(3);
        toCreate.setOneway(true);
        toCreate.setAdditionalInfo(JacksonUtil.toJsonNode("{\"src\":\"test\"}"));

        assertThat(create(toCreate)).isTrue();

        // Every column the native INSERT binds must round-trip - a missed column would silently write NULL.
        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.QUEUED);
        assertThat(stored.getRequestId()).isEqualTo(3);
        assertThat(stored.getOneway()).isTrue();
        assertThat(stored.getCreatedTime()).isEqualTo(toCreate.getCreatedTime());
        assertThat(stored.getExpirationTime()).isEqualTo(toCreate.getExpirationTime());
        assertThat(stored.getDeviceId()).isEqualTo(deviceId);
        assertThat(stored.getRequest()).isEqualTo(JacksonUtil.toJsonNode("{\"method\":\"x\"}"));
        assertThat(stored.getAdditionalInfo()).isEqualTo(JacksonUtil.toJsonNode("{\"src\":\"test\"}"));
        assertThat(stored.getResponse()).isNull();
    }

    @Test
    public void saveIsUnsupportedSoTheUnguardedMergeStaysUnreachable() {
        UUID id = UUID.randomUUID();
        Rpc toSave = rpc(id, new DeviceId(UUID.randomUUID()), RpcStatus.QUEUED, null);

        // The JPA merge is the bug this whole path replaces: it would upsert, clobbering a row an earlier
        // delivery of the same rpcId created. Both entry points must stay closed - saveAndFlush does NOT
        // delegate to the public save(), so overriding one would not cover the other.
        assertThatThrownBy(() -> rpcDao.save(TenantId.SYS_TENANT_ID, toSave))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, toSave))
                .isInstanceOf(UnsupportedOperationException.class);

        // ...and neither attempt wrote anything.
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id)).isNull();
    }

    @Test
    public void createIfAbsentIsNoOpWhenRowExists() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Rpc original = rpc(id, deviceId, RpcStatus.QUEUED, null);
        original.setRequestId(1);
        assertThat(create(original)).isTrue();

        // A re-delivered command carries the SAME rpcId but a fresh rpcSeq and a later createdTime.
        // The old JPA merge overwrote both; insert-if-absent must change nothing at all.
        Rpc duplicate = rpc(id, deviceId, RpcStatus.QUEUED, null);
        duplicate.setRequestId(99);
        duplicate.setCreatedTime(original.getCreatedTime() + 5_000);
        assertThat(create(duplicate)).isFalse();

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored.getRequestId()).isEqualTo(1);
        assertThat(stored.getCreatedTime()).isEqualTo(original.getCreatedTime());
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.QUEUED);
    }

    @Test
    public void createIfAbsentDoesNotResurrectTerminalRow() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        assertThat(create(rpc(id, deviceId, RpcStatus.QUEUED, null))).isTrue();

        // The two write paths compose: insert-if-absent creates, the guarded UPDATE completes.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"ok\":true}")))
                .get(5, TimeUnit.SECONDS)).isTrue();

        // The reported bug: a re-delivered create used to merge the finished row back to QUEUED with a new
        // requestId, producing a second in-flight attempt against one row.
        assertThat(create(rpc(id, deviceId, RpcStatus.QUEUED, null))).isFalse();

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.SUCCESSFUL);
        assertThat(stored.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
    }

    @Test
    public void findInFlightForReloadExcludesOneWayDeliveredAndTerminal() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID q = seedRpc(deviceId, RpcStatus.QUEUED, false);
        UUID s = seedRpc(deviceId, RpcStatus.SENT, true);          // one-way SENT still reloaded (retry)
        UUID t = seedRpc(deviceId, RpcStatus.TIMEOUT, false);      // delivery ack timed out -> in-flight, reloaded
        UUID twoWayDel = seedRpc(deviceId, RpcStatus.DELIVERED, false);
        seedRpc(deviceId, RpcStatus.DELIVERED, true);              // one-way DELIVERED -> excluded
        seedRpc(deviceId, RpcStatus.SUCCESSFUL, false);            // terminal -> excluded

        List<UUID> got = rpcDao.findInFlightForReload(TenantId.SYS_TENANT_ID, deviceId, new PageLink(100))
                .getData().stream().map(Rpc::getUuidId).toList();

        assertThat(got).containsExactlyInAnyOrder(q, s, t, twoWayDel);
    }

    @Test
    public void guardAllowsSuccessfulOnDeliveredRowWithNullOneway() throws Exception {
        UUID id = UUID.randomUUID();
        rawSeededId = id;
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        // Legacy row: oneway column NULL (pre-4.2.2.4). Raw insert because the entity writes a primitive boolean.
        jdbcTemplate.update("INSERT INTO rpc (id, created_time, tenant_id, device_id, expiration_time, request, " +
                        "response, status, request_id, oneway) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, NULL)",
                id, System.currentTimeMillis(), TenantId.SYS_TENANT_ID.getId(), deviceId.getId(),
                System.currentTimeMillis() + 60_000, "{\"method\":\"x\"}", "DELIVERED", 1);

        // NULL oneway must be treated as two-way (not terminal): a real response may complete it.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"ok\":true}")))
                .get(5, TimeUnit.SECONDS)).isTrue();
        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.SUCCESSFUL);
        assertThat(stored.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
    }

    private UUID seedRpc(DeviceId deviceId, RpcStatus status, boolean oneway) throws Exception {
        UUID id = UUID.randomUUID();
        Rpc toCreate = rpc(id, deviceId, status, null);
        toCreate.setOneway(oneway);
        toCreate.setRequestId(null);
        toCreate.setExpirationTime(System.currentTimeMillis() + 60_000);
        create(toCreate);
        return id;
    }

    // Seeds a row and waits for its flush, so a test can rely on it existing before asserting.
    private boolean create(Rpc rpc) throws Exception {
        return rpcDao.createIfAbsentAsync(rpc).get(5, TimeUnit.SECONDS);
    }

    private Rpc rpc(UUID id, DeviceId deviceId, RpcStatus status, JsonNode response) {
        return rpc(TenantId.SYS_TENANT_ID, id, deviceId, status, response);
    }

    private Rpc rpc(TenantId tenantId, UUID id, DeviceId deviceId, RpcStatus status, JsonNode response) {
        Rpc rpc = new Rpc(new RpcId(id));
        rpc.setCreatedTime(System.currentTimeMillis());
        rpc.setTenantId(tenantId);
        rpc.setDeviceId(deviceId);
        rpc.setExpirationTime(System.currentTimeMillis() + 60_000);
        rpc.setRequest(JacksonUtil.toJsonNode("{\"method\":\"x\"}"));
        rpc.setStatus(status);
        rpc.setResponse(response);
        return rpc;
    }

    @Test
    public void oneBatchReportsPerRowWhetherEachInsertApplied() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();

        create(rpc(idA, deviceId, RpcStatus.QUEUED, null));

        List<Boolean> results = rpcWriteRepository.write(List.of(
                RpcWrite.insert(new RpcEntity(rpc(idA, deviceId, RpcStatus.QUEUED, null))),
                RpcWrite.insert(new RpcEntity(rpc(idB, deviceId, RpcStatus.QUEUED, null)))));

        // A conflicted insert reported as true is how a redelivered command gets sent to the device twice.
        assertThat(results).containsExactly(false, true);
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, idB)).isNotNull();
    }

    @Test
    public void oneBatchTreatsADuplicateRpcIdInsideItAsAConflict() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID id = UUID.randomUUID();

        // A redelivered command coalesced into one flush: the second must conflict, not raise.
        assertThat(rpcWriteRepository.write(List.of(
                RpcWrite.insert(new RpcEntity(rpc(id, deviceId, RpcStatus.QUEUED, null))),
                RpcWrite.insert(new RpcEntity(rpc(id, deviceId, RpcStatus.QUEUED, null))))))
                .containsExactly(true, false);
    }

    @Test
    public void oneBatchAppliesTheInsertBeforeAnUpdateForTheSameRpcId() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID id = UUID.randomUUID();

        // Submitted update-first, so ordering must come from the operation tag and not from list position:
        // update-then-insert would match no row and strand a QUEUED one.
        List<Boolean> results = rpcWriteRepository.write(List.of(
                RpcWrite.update(new RpcEntity(rpc(id, deviceId, RpcStatus.DELIVERED, null))),
                RpcWrite.insert(new RpcEntity(rpc(id, deviceId, RpcStatus.QUEUED, null)))));

        assertThat(results).containsExactly(true, true);

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.DELIVERED);
    }

    @Test
    public void batchOfOnlyInsertsAndBatchOfOnlyUpdatesBothApply() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID id = UUID.randomUUID();

        // Guards the empty-partition branches: a homogeneous batch must not index into an empty result array.
        assertThat(rpcWriteRepository.write(List.of(
                RpcWrite.insert(new RpcEntity(rpc(id, deviceId, RpcStatus.QUEUED, null))))))
                .containsExactly(true);

        assertThat(rpcWriteRepository.write(List.of(
                RpcWrite.update(new RpcEntity(rpc(id, deviceId, RpcStatus.SENT, null))))))
                .containsExactly(true);
    }

    @Test
    public void createAndUpdateForTheSameRpcIdResolveInOrderThroughTheSharedQueue() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID id = UUID.randomUUID();

        // Same rpcId stripe, so the update cannot be applied before the insert.
        var created = rpcDao.createIfAbsentAsync(rpc(id, deviceId, RpcStatus.QUEUED, null));
        var updated = rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.DELIVERED, null));

        assertThat(created.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(updated.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id).getStatus()).isEqualTo(RpcStatus.DELIVERED);
    }
}
