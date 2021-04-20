/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcResponse;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.queue.TbClusterService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineRpcService implements TbRuleEngineDeviceRpcService {

    private final PartitionService partitionService;
    private final TbClusterService clusterService;
    private final TbServiceInfoProvider serviceInfoProvider;

    private final ConcurrentMap<UUID, Consumer<FromDeviceRpcResponse>> toDeviceRpcRequests = new ConcurrentHashMap<>();

    private Optional<TbCoreDeviceRpcService> tbCoreRpcService;
    private ScheduledExecutorService scheduler;
    private String serviceId;

    public DefaultTbRuleEngineRpcService(PartitionService partitionService,
                                         TbClusterService clusterService,
                                         TbServiceInfoProvider serviceInfoProvider) {
        this.partitionService = partitionService;
        this.clusterService = clusterService;
        this.serviceInfoProvider = serviceInfoProvider;
    }

    @Autowired(required = false)
    public void setTbCoreRpcService(Optional<TbCoreDeviceRpcService> tbCoreRpcService) {
        this.tbCoreRpcService = tbCoreRpcService;
    }

    @PostConstruct
    public void initExecutor() {
        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("rule-engine-rpc-scheduler"));
        serviceId = serviceInfoProvider.getServiceId();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void sendRpcReplyToDevice(String serviceId, UUID sessionId, int requestId, String body) {
        TransportProtos.ToServerRpcResponseMsg responseMsg = TransportProtos.ToServerRpcResponseMsg.newBuilder()
                .setRequestId(requestId)
                .setPayload(body).build();
        TransportProtos.ToTransportMsg msg = TransportProtos.ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToServerResponse(responseMsg)
                .build();
        clusterService.pushNotificationToTransport(serviceId, msg, null);
    }

    @Override
    public void sendRpcRequestToDevice(RuleEngineDeviceRpcRequest src, Consumer<RuleEngineDeviceRpcResponse> consumer) {
        ToDeviceRpcRequest request = new ToDeviceRpcRequest(src.getRequestUUID(), src.getTenantId(), src.getDeviceId(),
                src.isOneway(), src.getExpirationTime(), new ToDeviceRpcRequestBody(src.getMethod(), src.getBody()));
        forwardRpcRequestToDeviceActor(request, response -> {
            if (src.isRestApiCall()) {
                sendRpcResponseToTbCore(src.getOriginServiceId(), response);
            }
            consumer.accept(RuleEngineDeviceRpcResponse.builder()
                    .deviceId(src.getDeviceId())
                    .requestId(src.getRequestId())
                    .error(response.getError())
                    .response(response.getResponse())
                    .build());
        });
    }

    @Override
    public void processRpcResponseFromDevice(FromDeviceRpcResponse response) {
        log.trace("[{}] Received response to server-side RPC request from Core RPC Service", response.getId());
        UUID requestId = response.getId();
        Consumer<FromDeviceRpcResponse> consumer = toDeviceRpcRequests.remove(requestId);
        if (consumer != null) {
            scheduler.submit(() -> consumer.accept(response));
        } else {
            log.trace("[{}] Unknown or stale rpc response received [{}]", requestId, response);
        }
    }

    private void forwardRpcRequestToDeviceActor(ToDeviceRpcRequest request, Consumer<FromDeviceRpcResponse> responseConsumer) {
        log.trace("[{}][{}] Processing local rpc call to device actor [{}]", request.getTenantId(), request.getId(), request.getDeviceId());
        UUID requestId = request.getId();
        toDeviceRpcRequests.put(requestId, responseConsumer);
        sendRpcRequestToDevice(request);
        scheduleTimeout(request, requestId);
    }

    private void sendRpcRequestToDevice(ToDeviceRpcRequest msg) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, msg.getTenantId(), msg.getDeviceId());
        ToDeviceRpcRequestActorMsg rpcMsg = new ToDeviceRpcRequestActorMsg(serviceId, msg);
        if (tpi.isMyPartition()) {
            log.trace("[{}] Forwarding msg {} to device actor!", msg.getDeviceId(), msg);
            if (tbCoreRpcService.isPresent()) {
                tbCoreRpcService.get().forwardRpcRequestToDeviceActor(rpcMsg);
            } else {
                log.warn("Failed to find tbCoreRpcService for local service. Possible duplication of serviceIds.");
            }
        } else {
            log.trace("[{}] Forwarding msg {} to queue actor!", msg.getDeviceId(), msg);
            clusterService.pushMsgToCore(rpcMsg, null);
        }
    }

    private void sendRpcResponseToTbCore(String originServiceId, FromDeviceRpcResponse response) {
        if (serviceId.equals(originServiceId)) {
            if (tbCoreRpcService.isPresent()) {
                tbCoreRpcService.get().processRpcResponseFromRuleEngine(response);
            } else {
                log.warn("Failed to find tbCoreRpcService for local service. Possible duplication of serviceIds.");
            }
        } else {
            clusterService.pushNotificationToCore(originServiceId, response, null);
        }
    }

    private void scheduleTimeout(ToDeviceRpcRequest request, UUID requestId) {
        long timeout = Math.max(0, request.getExpirationTime() - System.currentTimeMillis()) + TimeUnit.SECONDS.toMillis(1);
        log.trace("[{}] processing the request: [{}]", this.hashCode(), requestId);
        scheduler.schedule(() -> {
            log.trace("[{}] timeout the request: [{}]", this.hashCode(), requestId);
            Consumer<FromDeviceRpcResponse> consumer = toDeviceRpcRequests.remove(requestId);
            if (consumer != null) {
                scheduler.submit(() -> consumer.accept(new FromDeviceRpcResponse(requestId, null, RpcError.TIMEOUT)));
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }
}
