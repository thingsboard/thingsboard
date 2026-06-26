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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.AbstractJpaDaoTest;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaRpcDaoTest extends AbstractJpaDaoTest {

    @Autowired
    JpaRpcDao rpcDao;

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
    public void saveAsyncInsertThenUpsert() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        // A create always persists (INSERT ... ON CONFLICT), so the future resolves true.
        assertThat(rpcDao.createAsync(rpc(id, deviceId, RpcStatus.QUEUED, null)).get(5, TimeUnit.SECONDS)).isTrue();

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.QUEUED);
        assertThat(stored.getResponse()).isNull();

        // The update matches the existing row, so the future resolves true.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.DELIVERED, JacksonUtil.toJsonNode("{\"ok\":true}")))
                .get(5, TimeUnit.SECONDS)).isTrue();

        Rpc afterUpdate = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(afterUpdate.getStatus()).isEqualTo(RpcStatus.DELIVERED);
        assertThat(afterUpdate.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
    }

    @Test
    public void saveAsyncSameRpcIdCoalescedBatchKeepsLatestStatus() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long createdTime = System.currentTimeMillis();
        long expirationTime = createdTime + 60_000;

        Rpc queued = new Rpc(new RpcId(id));
        queued.setCreatedTime(createdTime);
        queued.setTenantId(TenantId.SYS_TENANT_ID);
        queued.setDeviceId(deviceId);
        queued.setExpirationTime(expirationTime);
        queued.setRequest(JacksonUtil.toJsonNode("{\"method\":\"x\"}"));
        queued.setStatus(RpcStatus.QUEUED);

        Rpc delivered = new Rpc(new RpcId(id));
        delivered.setCreatedTime(createdTime);
        delivered.setTenantId(TenantId.SYS_TENANT_ID);
        delivered.setDeviceId(deviceId);
        delivered.setExpirationTime(expirationTime);
        delivered.setRequest(queued.getRequest());
        delivered.setStatus(RpcStatus.DELIVERED);
        delivered.setResponse(JacksonUtil.toJsonNode("{\"ok\":true}"));

        // Enqueue both writes for the same rpcId back-to-back so they coalesce into one flush batch.
        // Same rpcId -> same partition; the queue's stable sort must keep submission order
        // (QUEUED before DELIVERED), so the final persisted row must be DELIVERED, never QUEUED.
        var queuedFuture = rpcDao.createAsync(queued);
        var deliveredFuture = rpcDao.updateAsync(delivered);
        queuedFuture.get(5, TimeUnit.SECONDS);
        deliveredFuture.get(5, TimeUnit.SECONDS);

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.DELIVERED);
        assertThat(stored.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
    }

    @Test
    public void saveAsyncRequeueUpdatesExistingRowToQueued() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        // Initial create.
        rpcDao.createAsync(rpc(id, deviceId, RpcStatus.QUEUED, null)).get(5, TimeUnit.SECONDS);

        // Delivery timeout with closeTransportSessionOnRpcDeliveryTimeout=true re-queues the RPC: the
        // device actor persists status=QUEUED again as a status update so init() can re-pick it up.
        // The update must land on the existing row, never be dropped.
        rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.QUEUED, null)).get(5, TimeUnit.SECONDS);

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.QUEUED);
    }

    @Test
    public void saveAsyncNullResponseUpdateKeepsStoredResponse() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        rpcDao.createAsync(rpc(id, deviceId, RpcStatus.QUEUED, null)).get(5, TimeUnit.SECONDS);

        // A successful response is stored.
        rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"ok\":true}")))
                .get(5, TimeUnit.SECONDS);

        // A later status update carries no response - it must NOT clobber the stored one.
        rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.EXPIRED, null)).get(5, TimeUnit.SECONDS);

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.EXPIRED);
        assertThat(stored.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
    }

    @Test
    public void saveAsyncUpdateForDeletedRpcDoesNotResurrect() throws Exception {
        UUID id = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        rpcDao.createAsync(rpc(id, deviceId, RpcStatus.QUEUED, null)).get(5, TimeUnit.SECONDS);

        // RPC is removed (TTL cleanup / manual delete) while a response is still in flight.
        rpcDao.removeById(TenantId.SYS_TENANT_ID, id);
        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id)).isNull();

        // A late status update must not re-create the deleted row, and the future must resolve false
        // (no row matched) so the service layer can skip the rule-engine notification.
        assertThat(rpcDao.updateAsync(rpc(id, deviceId, RpcStatus.SUCCESSFUL, JacksonUtil.toJsonNode("{\"ok\":true}")))
                .get(5, TimeUnit.SECONDS)).isFalse();

        assertThat(rpcDao.findById(TenantId.SYS_TENANT_ID, id)).isNull();
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
