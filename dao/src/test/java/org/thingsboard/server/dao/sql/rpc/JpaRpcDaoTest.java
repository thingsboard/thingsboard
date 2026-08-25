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

public class JpaRpcDaoTest extends AbstractJpaDaoTest {

    @Autowired
    JpaRpcDao rpcDao;

    @Autowired
    RpcUpdateRepository rpcUpdateRepository;

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
    public void deleteOutdated() {
        Rpc rpc = new Rpc();
        rpc.setTenantId(TenantId.SYS_TENANT_ID);
        rpc.setDeviceId(new DeviceId(UUID.randomUUID()));
        rpc.setStatus(RpcStatus.QUEUED);
        rpc.setRequest(JacksonUtil.toJsonNode("{}"));
        rpcDao.saveAndFlush(rpc.getTenantId(), rpc);

        rpc.setId(null);
        rpcDao.saveAndFlush(rpc.getTenantId(), rpc);

        TenantId tenantId = TenantId.fromUUID(UUID.fromString("3d193a7a-774b-4c05-84d5-f7fdcf7a37cf"));
        rpc.setId(null);
        rpc.setTenantId(tenantId);
        rpc.setDeviceId(new DeviceId(UUID.randomUUID()));
        rpcDao.saveAndFlush(rpc.getTenantId(), rpc);

        int batchSize = 10_000;
        assertThat(rpcDao.deleteOutdatedRpcByTenantIdBatch(TenantId.SYS_TENANT_ID, 0L, batchSize)).isEqualTo(0);
        assertThat(rpcDao.deleteOutdatedRpcByTenantIdBatch(TenantId.SYS_TENANT_ID, Long.MAX_VALUE, batchSize)).isEqualTo(2);
        assertThat(rpcDao.deleteOutdatedRpcByTenantIdBatch(tenantId, System.currentTimeMillis() + 1, batchSize)).isEqualTo(1);
    }

    @Test
    public void syncCreateThenAsyncUpdateConvergesToUpdatedStatus() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        // Production create path: the QUEUED row is persisted synchronously (persist-before-send).
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, rpc(id, deviceId, RpcStatus.QUEUED, null));

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
    public void updateBatchAppliesInOrderAndAlignsResults() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID(); // never created -> its update must report no match

        // Row A exists (persisted synchronously at create time); B was never created.
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, rpc(idA, deviceId, RpcStatus.QUEUED, null));

        // Drive the persist logic directly with a single, deterministically-coalesced update batch.
        // This is exactly what "coalescing" means: one update() call carrying several writes for the
        // same partition in submission order. No queue timing involved.
        //   index 0: update A (SUCCESSFUL, {ok:true}) -> UPDATE hits the existing row  -> true
        //   index 1: update B (SUCCESSFUL, {x:1})     -> UPDATE for a missing row      -> false
        //   index 2: update A (EXPIRED) -> guard blocks overwrite of terminal SUCCESSFUL -> false
        List<RpcEntity> batch = List.of(
                new RpcEntity(rpc(idA, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"ok\":true}"))),
                new RpcEntity(rpc(idB, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"x\":1}"))),
                new RpcEntity(rpc(idA, deviceId, RpcStatus.EXPIRED, null)));

        List<Boolean> persisted = rpcUpdateRepository.update(batch);

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
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, rpc(id, deviceId, RpcStatus.QUEUED, null));

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

        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, rpc(id, deviceId, RpcStatus.QUEUED, null));

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
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, created);

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
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, created);

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
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, rpc(id, deviceId, RpcStatus.SENT, null));

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
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, created);

        // A stale/duplicate SENT must not roll DELIVERED backwards.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SENT, null)).get(5, TimeUnit.SECONDS)).isFalse();
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id).getStatus()).isEqualTo(RpcStatus.DELIVERED);
    }

    @Test
    public void saveAsyncUpdateForDeletedRpcDoesNotResurrect() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, rpc(id, deviceId, RpcStatus.QUEUED, null));

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
    public void requestIdRoundTripsThroughSaveAndLoad() {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Rpc toSave = rpc(id, deviceId, RpcStatus.SENT, null);
        toSave.setRequestId(42);
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, toSave);

        Rpc loaded = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(loaded.getRequestId()).isEqualTo(42);
    }

    @Test
    public void onewayRoundTripsThroughSaveAndLoad() {
        UUID id = UUID.randomUUID();
        Rpc toSave = rpc(id, new DeviceId(UUID.randomUUID()), RpcStatus.SENT, null);
        toSave.setOneway(true);
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, toSave);
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id).getOneway()).isTrue();
    }

    @Test
    public void findInFlightForReloadExcludesOneWayDeliveredAndTerminal() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        UUID q = saveRpc(deviceId, RpcStatus.QUEUED, false);
        UUID s = saveRpc(deviceId, RpcStatus.SENT, true);          // one-way SENT still reloaded (retry)
        UUID t = saveRpc(deviceId, RpcStatus.TIMEOUT, false);      // delivery ack timed out -> in-flight, reloaded
        UUID twoWayDel = saveRpc(deviceId, RpcStatus.DELIVERED, false);
        saveRpc(deviceId, RpcStatus.DELIVERED, true);              // one-way DELIVERED -> excluded
        saveRpc(deviceId, RpcStatus.SUCCESSFUL, false);            // terminal -> excluded

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

    private UUID saveRpc(DeviceId deviceId, RpcStatus status, boolean oneway) {
        UUID id = UUID.randomUUID();
        Rpc toSave = rpc(id, deviceId, status, null);
        toSave.setOneway(oneway);
        toSave.setRequestId(null);
        toSave.setExpirationTime(System.currentTimeMillis() + 60_000);
        rpcDao.saveAndFlush(TenantId.SYS_TENANT_ID, toSave);
        return id;
    }

    private Rpc rpc(UUID id, DeviceId deviceId, RpcStatus status, JsonNode response) {
        Rpc rpc = new Rpc(new RpcId(id));
        rpc.setCreatedTime(System.currentTimeMillis());
        rpc.setTenantId(TenantId.SYS_TENANT_ID);
        rpc.setDeviceId(deviceId);
        rpc.setExpirationTime(System.currentTimeMillis() + 60_000);
        rpc.setRequest(JacksonUtil.toJsonNode("{\"method\":\"x\"}"));
        rpc.setStatus(status);
        rpc.setResponse(response);
        return rpc;
    }

}
