// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.rpc;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.AbstractJpaDaoTest;

import java.util.UUID;

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

}
