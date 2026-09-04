// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ruleengine;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
public class DefaultRuleEngineCallService implements RuleEngineCallService {

    private final TbClusterService clusterService;

    private ScheduledExecutorService executor;

    private final ConcurrentMap<UUID, Consumer<TbMsg>> requests = new ConcurrentHashMap<>();

    public DefaultRuleEngineCallService(TbClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @PostConstruct
    public void initExecutor() {
        executor = ThingsBoardExecutors.newSingleThreadScheduledExecutor("re-rest-callback");
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void processRestApiCallToRuleEngine(TenantId tenantId, UUID requestId, TbMsg request, boolean useQueueFromTbMsg, Consumer<TbMsg> responseConsumer) {
        log.trace("[{}] Processing REST API call to rule engine: [{}] for entity: [{}]", tenantId, requestId, request.getOriginator());
        requests.put(requestId, responseConsumer);
        sendRequestToRuleEngine(tenantId, request, useQueueFromTbMsg);
        scheduleTimeout(request, requestId, requests);
    }

    @Override
    public void onQueueMsg(TransportProtos.RestApiCallResponseMsgProto restApiCallResponseMsg, TbCallback callback) {
        UUID requestId = new UUID(restApiCallResponseMsg.getRequestIdMSB(), restApiCallResponseMsg.getRequestIdLSB());
        Consumer<TbMsg> consumer = requests.remove(requestId);
        if (consumer != null) {
            consumer.accept(TbMsg.fromProto(null, restApiCallResponseMsg.getResponseProto(), TbMsgCallback.EMPTY));
        } else {
            log.trace("[{}] Unknown or stale rest api call response received", requestId);
        }
        callback.onSuccess();
    }

    private void sendRequestToRuleEngine(TenantId tenantId, TbMsg msg, boolean useQueueFromTbMsg) {
        clusterService.pushMsgToRuleEngine(tenantId, msg.getOriginator(), msg, useQueueFromTbMsg, null);
    }

    private void scheduleTimeout(TbMsg request, UUID requestId, ConcurrentMap<UUID, Consumer<TbMsg>> requestsMap) {
        long expirationTime = Long.parseLong(request.getMetaData().getValue("expirationTime"));
        long timeout = Math.max(0, expirationTime - System.currentTimeMillis());
        log.trace("[{}] processing the request: [{}]", this.hashCode(), requestId);
        executor.schedule(() -> {
            Consumer<TbMsg> consumer = requestsMap.remove(requestId);
            if (consumer != null) {
                log.trace("[{}] request timeout detected: [{}]", this.hashCode(), requestId);
                consumer.accept(null);
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }
}
