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

import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.HashPartitioner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TbCoreComponent
@Service
@Slf4j
public class TbRpcService {
    private final RpcService rpcService;
    private final TbClusterService tbClusterService;

    // Post-persist rule-engine notifications run on these striped single-thread executors, keyed by
    // rpcId, instead of inline on the SQL persist threads. A flush batch can carry up to batch_size
    // entries, and each notification's pushMsgToRuleEngine may do a (potentially blocking) device-profile
    // cache lookup; running them inline would serialize that work on the persist thread and stall the
    // next DB flush. Striping by rpcId (rather than a shared multi-threaded pool) also preserves
    // per-command notification order, e.g. RPC_QUEUED before RPC_DELIVERED.
    private final ExecutorService[] callbackExecutors;

    public TbRpcService(RpcService rpcService, TbClusterService tbClusterService,
                        @Value("${sql.rpc.callback_threads:3}") int callbackThreads) {
        this.rpcService = rpcService;
        this.tbClusterService = tbClusterService;
        this.callbackExecutors = new ExecutorService[callbackThreads];
        for (int i = 0; i < callbackThreads; i++) {
            callbackExecutors[i] = Executors.newSingleThreadExecutor(
                    ThingsBoardThreadFactory.forName("rpc-persist-callback-" + i));
        }
    }

    @PreDestroy
    private void destroy() {
        for (ExecutorService executor : callbackExecutors) {
            executor.shutdownNow();
        }
    }

    public void create(TenantId tenantId, Rpc rpc) {
        persist(tenantId, rpc, rpcService.createAsync(rpc));
    }

    public void update(TenantId tenantId, Rpc rpc) {
        persist(tenantId, rpc, rpcService.updateAsync(rpc));
    }

    private void persist(TenantId tenantId, Rpc rpc, ListenableFuture<Boolean> future) {
        DonAsynchron.withCallback(future,
                persisted -> {
                    if (Boolean.TRUE.equals(persisted)) {
                        pushRpcMsgToRuleEngine(tenantId, rpc);
                    } else {
                        log.debug("[{}][{}][{}] Skipping rule engine notification for status [{}] - RPC row no longer exists",
                                tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus());
                    }
                },
                t -> log.error("[{}][{}][{}] Failed to persist RPC with status [{}]",
                        tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus(), t),
                executorFor(rpc.getUuidId()));
    }

    private Executor executorFor(UUID rpcId) {
        return callbackExecutors[HashPartitioner.resolvePartition(rpcId.hashCode(), callbackExecutors.length)];
    }

    private void pushRpcMsgToRuleEngine(TenantId tenantId, Rpc rpc) {
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.valueOf("RPC_" + rpc.getStatus().name()))
                .originator(rpc.getDeviceId())
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(JacksonUtil.toString(rpc))
                .build();
        tbClusterService.pushMsgToRuleEngine(tenantId, rpc.getDeviceId(), msg, null);
    }

    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        return rpcService.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
    }

}
