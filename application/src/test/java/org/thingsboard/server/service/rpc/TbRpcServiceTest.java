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
package org.thingsboard.server.service.rpc;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.rpc.RpcService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbRpcServiceTest {

    private final RpcService rpcService = mock(RpcService.class);
    private final TbClusterService clusterService = mock(TbClusterService.class);
    private TbRpcService tbRpcService;

    @Before
    public void setUp() {
        tbRpcService = new TbRpcService(rpcService, clusterService);
        ReflectionTestUtils.setField(tbRpcService, "callbackThreads", 1);
        ReflectionTestUtils.invokeMethod(tbRpcService, "init");
    }

    @Test
    public void savePersistsViaUpdateAsyncThenPushesToRuleEngine() {
        Rpc rpc = newRpc();
        when(rpcService.updateAsync(rpc)).thenReturn(Futures.immediateFuture(null));

        tbRpcService.save(rpc.getTenantId(), rpc);

        verify(rpcService).updateAsync(rpc);
        verify(clusterService, timeout(5000))
                .pushMsgToRuleEngine(eq(rpc.getTenantId()), eq(rpc.getDeviceId()), any(TbMsg.class), isNull());
    }

    @Test
    public void createPersistsViaCreateAsyncThenPushesToRuleEngine() {
        Rpc rpc = newRpc();
        when(rpcService.createAsync(rpc)).thenReturn(Futures.immediateFuture(null));

        tbRpcService.create(rpc.getTenantId(), rpc);

        verify(rpcService).createAsync(rpc);
        verify(clusterService, timeout(5000))
                .pushMsgToRuleEngine(eq(rpc.getTenantId()), eq(rpc.getDeviceId()), any(TbMsg.class), isNull());
    }

    private Rpc newRpc() {
        Rpc rpc = new Rpc(new RpcId(UUID.randomUUID()));
        rpc.setTenantId(TenantId.SYS_TENANT_ID);
        rpc.setDeviceId(new DeviceId(UUID.randomUUID()));
        rpc.setStatus(RpcStatus.QUEUED);
        rpc.setRequest(JacksonUtil.toJsonNode("{}"));
        return rpc;
    }
}
