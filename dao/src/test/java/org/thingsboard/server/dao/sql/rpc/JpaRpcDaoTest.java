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
        Rpc rpc = new Rpc(new RpcId(id));
        rpc.setCreatedTime(System.currentTimeMillis());
        rpc.setTenantId(TenantId.SYS_TENANT_ID);
        rpc.setDeviceId(new DeviceId(UUID.randomUUID()));
        rpc.setExpirationTime(System.currentTimeMillis() + 60_000);
        rpc.setRequest(JacksonUtil.toJsonNode("{\"method\":\"x\"}"));
        rpc.setStatus(RpcStatus.QUEUED);

        rpcDao.saveAsync(rpc.getTenantId(), rpc).get(5, TimeUnit.SECONDS);

        Rpc stored = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(RpcStatus.QUEUED);
        assertThat(stored.getResponse()).isNull();

        Rpc update = new Rpc(new RpcId(id));
        update.setCreatedTime(rpc.getCreatedTime());
        update.setTenantId(TenantId.SYS_TENANT_ID);
        update.setDeviceId(rpc.getDeviceId());
        update.setExpirationTime(rpc.getExpirationTime());
        update.setRequest(rpc.getRequest());
        update.setStatus(RpcStatus.DELIVERED);
        update.setResponse(JacksonUtil.toJsonNode("{\"ok\":true}"));

        rpcDao.saveAsync(update.getTenantId(), update).get(5, TimeUnit.SECONDS);

        Rpc afterUpdate = rpcDao.findById(TenantId.SYS_TENANT_ID, id);
        assertThat(afterUpdate.getStatus()).isEqualTo(RpcStatus.DELIVERED);
        assertThat(afterUpdate.getResponse()).isEqualTo(JacksonUtil.toJsonNode("{\"ok\":true}"));
    }

}
