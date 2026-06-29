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
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.rpc.RpcService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
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
        tbRpcService = new TbRpcService(rpcService, clusterService, 1);
    }

    @Test
    public void updatePersistsViaUpdateAsyncThenPushesToRuleEngine() {
        Rpc rpc = newRpc();
        when(rpcService.updateAsync(rpc)).thenReturn(Futures.immediateFuture(true));

        tbRpcService.update(rpc.getTenantId(), rpc);

        verify(rpcService).updateAsync(rpc);
        verify(clusterService, timeout(5000))
                .pushMsgToRuleEngine(eq(rpc.getTenantId()), eq(rpc.getDeviceId()), any(TbMsg.class), isNull());
    }

    @Test
    public void createPersistsViaCreateAsyncThenPushesToRuleEngine() {
        Rpc rpc = newRpc();
        when(rpcService.createAsync(rpc)).thenReturn(Futures.immediateFuture(true));

        tbRpcService.create(rpc.getTenantId(), rpc);

        verify(rpcService).createAsync(rpc);
        verify(clusterService, timeout(5000))
                .pushMsgToRuleEngine(eq(rpc.getTenantId()), eq(rpc.getDeviceId()), any(TbMsg.class), isNull());
    }

    @Test
    public void updateDoesNotNotifyRuleEngineWhenRowMissing() {
        Rpc rpc = newRpc();
        // updateAsync resolves false: the UPDATE matched no row (RPC was deleted), so the rule engine
        // must not be notified for a status change that never persisted.
        when(rpcService.updateAsync(rpc)).thenReturn(Futures.immediateFuture(false));

        tbRpcService.update(rpc.getTenantId(), rpc);

        verify(rpcService).updateAsync(rpc);
        verify(clusterService, after(500).never())
                .pushMsgToRuleEngine(any(TenantId.class), any(DeviceId.class), any(TbMsg.class), isNull());
    }

    @Test
    public void sameRpcIdNotificationsRunInSubmissionOrderOnStripedExecutor() throws InterruptedException {
        // The count is incidental here: a single rpcId always maps to one stripe. What the test really
        // checks is that two notifications for the SAME rpcId are serialized on that one stripe.
        tbRpcService = new TbRpcService(rpcService, clusterService, 3);

        RpcId rpcId = new RpcId(UUID.randomUUID());
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Rpc queued = newRpc(rpcId, deviceId, RpcStatus.QUEUED);
        Rpc delivered = newRpc(rpcId, deviceId, RpcStatus.DELIVERED);
        when(rpcService.createAsync(queued)).thenReturn(Futures.immediateFuture(true));
        when(rpcService.updateAsync(delivered)).thenReturn(Futures.immediateFuture(true));

        // Make the QUEUED notification block while it runs: it signals that it has started, then sleeps -
        // holding the stripe. If the two callbacks for this rpcId were NOT serialized on one stripe, the
        // fast DELIVERED callback would overtake the sleeping QUEUED one and be recorded first.
        CountDownLatch queuedStarted = new CountDownLatch(1);
        doAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(2);
            if (msg.getInternalType() == TbMsgType.RPC_QUEUED) {
                queuedStarted.countDown();
                Thread.sleep(300);
            }
            return null;
        }).when(clusterService).pushMsgToRuleEngine(eq(TenantId.SYS_TENANT_ID), eq(deviceId), any(TbMsg.class), isNull());

        tbRpcService.create(queued.getTenantId(), queued);
        // Don't submit DELIVERED until QUEUED is actually in flight (and now sleeping) on the stripe -
        // this makes the test about stripe serialization, not about submission timing.
        assertTrue(queuedStarted.await(5, TimeUnit.SECONDS));
        tbRpcService.update(delivered.getTenantId(), delivered);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(clusterService, timeout(5000).times(2))
                .pushMsgToRuleEngine(eq(TenantId.SYS_TENANT_ID), eq(deviceId), msgCaptor.capture(), isNull());

        List<TbMsg> msgs = msgCaptor.getAllValues();
        assertEquals(TbMsgType.RPC_QUEUED, msgs.get(0).getInternalType());
        assertEquals(TbMsgType.RPC_DELIVERED, msgs.get(1).getInternalType());
    }

    private Rpc newRpc() {
        return newRpc(new RpcId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), RpcStatus.QUEUED);
    }

    private Rpc newRpc(RpcId rpcId, DeviceId deviceId, RpcStatus status) {
        Rpc rpc = new Rpc(rpcId);
        rpc.setTenantId(TenantId.SYS_TENANT_ID);
        rpc.setDeviceId(deviceId);
        rpc.setStatus(status);
        rpc.setRequest(JacksonUtil.toJsonNode("{}"));
        return rpc;
    }
}
