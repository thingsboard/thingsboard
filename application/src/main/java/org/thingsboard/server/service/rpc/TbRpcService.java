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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.RpcPersistResult;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@TbCoreComponent
@Service
@Slf4j
public class TbRpcService {
    private final RpcService rpcService;
    private final TbClusterService tbClusterService;

    // Striped by rpcId so one command's lifecycle events keep their order (RPC_QUEUED before RPC_DELIVERED).
    private final ExecutorService[] ruleEngineCallbackExecutors;
    // Separate from the stripes above so rule engine publishing is never on the command-delivery path: a stall
    // there would otherwise hold up device sends. Needs no striping - the actor fixed delivery order already.
    private final ExecutorService deviceActorCallbackExecutor;

    public TbRpcService(RpcService rpcService, TbClusterService tbClusterService,
                        @Value("${sql.rpc.rule_engine_callback_threads:3}") int ruleEngineCallbackThreads,
                        @Value("${sql.rpc.device_actor_callback_threads:3}") int deviceActorCallbackThreads) {
        if (ruleEngineCallbackThreads < 1) {
            throw new IllegalArgumentException("sql.rpc.rule_engine_callback_threads must be >= 1, but was " + ruleEngineCallbackThreads);
        }
        if (deviceActorCallbackThreads < 1) {
            throw new IllegalArgumentException("sql.rpc.device_actor_callback_threads must be >= 1, but was " + deviceActorCallbackThreads);
        }
        this.rpcService = rpcService;
        this.tbClusterService = tbClusterService;
        this.ruleEngineCallbackExecutors = new ExecutorService[ruleEngineCallbackThreads];
        for (int i = 0; i < ruleEngineCallbackThreads; i++) {
            ruleEngineCallbackExecutors[i] = Executors.newSingleThreadExecutor(
                    ThingsBoardThreadFactory.forName("rpc-rule-engine-callback-" + i));
        }
        this.deviceActorCallbackExecutor = Executors.newFixedThreadPool(deviceActorCallbackThreads,
                ThingsBoardThreadFactory.forName("rpc-device-actor-callback"));
    }

    @PreDestroy
    private void destroy() {
        for (ExecutorService executor : ruleEngineCallbackExecutors) {
            executor.shutdownNow();
        }
        deviceActorCallbackExecutor.shutdownNow();
    }

    /**
     * Enqueues the create onto the batched write queue and resumes the caller once the row's fate is known. The
     * continuation is invoked exactly once.
     */
    public void createIfAbsent(TenantId tenantId, Rpc rpc, Consumer<RpcPersistResult> continuation) {
        DonAsynchron.withCallback(rpcService.createIfAbsentAsync(rpc),
                inserted -> {
                    RpcPersistResult result = Boolean.TRUE.equals(inserted)
                            ? RpcPersistResult.INSERTED : RpcPersistResult.DUPLICATE;
                    if (RpcPersistResult.INSERTED == result) {
                        // Enqueue before resuming the actor, so RPC_QUEUED sits on the stripe ahead of any
                        // status event the resumed actor causes. Only the enqueue happens here.
                        ruleEngineCallbackExecutorFor(rpc.getUuidId()).execute(() -> notifyRuleEngine(tenantId, rpc));
                    } else {
                        log.debug("[{}][{}][{}] Skipping RPC_QUEUED notification - a row for this RPC already existed",
                                tenantId, rpc.getDeviceId(), rpc.getId());
                    }
                    continuation.accept(result);
                },
                t -> {
                    log.error("[{}][{}][{}] Failed to persist RPC create with status [{}]",
                            tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus(), t);
                    continuation.accept(RpcPersistResult.FAILED);
                },
                deviceActorCallbackExecutor);
    }

    public void update(TenantId tenantId, Rpc rpc) {
        persist(tenantId, rpc, rpcService.updateAsync(rpc));
    }

    private void persist(TenantId tenantId, Rpc rpc, ListenableFuture<Boolean> future) {
        DonAsynchron.withCallback(future,
                persisted -> {
                    if (Boolean.TRUE.equals(persisted)) {
                        notifyRuleEngine(tenantId, rpc);
                    } else {
                        log.debug("[{}][{}][{}] Skipping rule engine notification for status [{}] - RPC row is not updatable (already terminal or removed)",
                                tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus());
                    }
                },
                t -> log.error("[{}][{}][{}] Failed to persist RPC with status [{}]",
                        tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus(), t),
                ruleEngineCallbackExecutorFor(rpc.getUuidId()));
    }

    private Executor ruleEngineCallbackExecutorFor(UUID rpcId) {
        return ruleEngineCallbackExecutors[HashPartitioner.resolvePartition(rpcId.hashCode(), ruleEngineCallbackExecutors.length)];
    }

    private void notifyRuleEngine(TenantId tenantId, Rpc rpc) {
        try {
            pushRpcMsgToRuleEngine(tenantId, rpc);
        } catch (Throwable t) {
            log.error("[{}][{}][{}] Failed to push RPC with status [{}] to rule engine",
                    tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus(), t);
        }
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

    public PageData<Rpc> findInFlightForReload(TenantId tenantId, DeviceId deviceId, PageLink pageLink) {
        return rpcService.findInFlightForReload(tenantId, deviceId, pageLink);
    }

}
