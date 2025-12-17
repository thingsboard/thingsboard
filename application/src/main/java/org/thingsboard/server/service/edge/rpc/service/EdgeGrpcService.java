/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.EdgeHighPriorityMsg;
import org.thingsboard.server.common.msg.edge.EdgeSessionMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.EdgeAttributeSaveCallback;
import org.thingsboard.server.service.edge.rpc.session.EdgeSessionsHolder;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_CONNECT_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_DISCONNECT_TIME;

@Service
@Slf4j
@RequiredArgsConstructor
public class EdgeGrpcService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase implements EdgeRpcService {

    @Value("${edges.send_scheduler_pool_size}")
    private int sendSchedulerPoolSize;

    @Value("${edges.state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @Autowired
    @Lazy
    private EdgeContextComponent ctx;

    private final EdgeSessionsHolder sessions;
    private final ApplicationContext applicationContext;
    private final TbClusterService clusterService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TelemetrySubscriptionService tsSubService;
    private final TbTransactionalCache<EdgeId, String> edgeIdServiceIdCache;

    private final ConcurrentMap<UUID, Consumer<FromEdgeSyncResponse>> localSyncEdgeRequests = new ConcurrentHashMap<>();
    private ScheduledExecutorService executorService;
    private ScheduledExecutorService sendDownlinkExecutorService;

    @PostConstruct
    public void onStartUp() {
        this.executorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("edge-service");
        this.sendDownlinkExecutorService = ThingsBoardExecutors.newScheduledThreadPool(sendSchedulerPoolSize, "edge-send-scheduler");
    }

    @PreDestroy
    private void preDestroy() {
        shutdownExecutorSafely(executorService);
        shutdownExecutorSafely(sendDownlinkExecutorService);
        sessions.forEach(EdgeGrpcSessionManager::onEdgeDisconnect);
    }

    @Override
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> outputStream) {
        EdgeGrpcSessionManager sessionManager = applicationContext.getBean(EdgeGrpcSessionManager.class);
        return sessionManager.initInputStream(outputStream, this::onEdgeConnect, this::onEdgeDisconnect, sendDownlinkExecutorService);
    }

    @Override
    public void onToEdgeSessionMsg(TenantId tenantId, EdgeSessionMsg msg) {
        switch (msg.getMsgType()) {
            case EDGE_HIGH_PRIORITY_TO_EDGE_SESSION_MSG -> {
                EdgeHighPriorityMsg edgeHighPriorityMsg = (EdgeHighPriorityMsg) msg;
                log.trace("[{}] edgeEventMsg [{}]", tenantId, msg);
                onEdgeHighPriorityEvent(edgeHighPriorityMsg);
            }
            case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG -> {
                EdgeEventUpdateMsg edgeEventUpdateMsg = (EdgeEventUpdateMsg) msg;
                log.trace("[{}] onToEdgeEventUpdateMsg [{}]", tenantId, msg);
                onEdgeEventUpdate(tenantId, edgeEventUpdateMsg.getEdgeId());
            }
            case EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG -> {
                ToEdgeSyncRequest toEdgeSyncRequest = (ToEdgeSyncRequest) msg;
                log.trace("[{}] toEdgeSyncRequest [{}]", tenantId, msg);
                startSyncProcess(tenantId, toEdgeSyncRequest.getEdgeId(), toEdgeSyncRequest.getId(), toEdgeSyncRequest.getServiceId());
            }
            case EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG -> {
                FromEdgeSyncResponse fromEdgeSyncResponse = (FromEdgeSyncResponse) msg;
                log.trace("[{}] fromEdgeSyncResponse [{}]", tenantId, msg);
                processSyncResponse(fromEdgeSyncResponse);
            }
        }
    }

    @Override
    public void updateEdge(TenantId tenantId, Edge edge) {
        if (edge == null) {
            log.warn("[{}] Edge is null - edge is removed and outdated notification is in process!", tenantId);
            return;
        }
        EdgeGrpcSessionManager session = sessions.getByEdgeId(edge.getId());
        if (session != null && session.getState().isConnected()) {
            log.debug("[{}] Updating configuration for edge [{}] [{}]", tenantId, edge.getName(), edge.getId());
            session.onConfigurationUpdate(edge);
        } else {
            log.debug("[{}] Session doesn't exist for edge [{}] [{}]", tenantId, edge.getName(), edge.getId());
        }
    }

    @Override
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        EdgeGrpcSessionManager session = sessions.getByEdgeId(edgeId);
        if (session != null && session.getState().isConnected()) {
            log.info("[{}] Closing and removing session for edge [{}]", tenantId, edgeId);
            session.destroyAndMarkAsZombieIfFailed();
            session.onEdgeRemoval();
            sessions.remove(session);
        }
    }

    @Override
    public void processSyncRequest(TenantId tenantId, EdgeId edgeId, Consumer<FromEdgeSyncResponse> responseConsumer) {
        ToEdgeSyncRequest request = new ToEdgeSyncRequest(UUID.randomUUID(), tenantId, edgeId, serviceInfoProvider.getServiceId());

        UUID requestId = request.getId();
        EdgeGrpcSessionManager session = sessions.getByEdgeId(request.getEdgeId());
        if (session != null && session.getState().isSyncInProgress()) {
            responseConsumer.accept(new FromEdgeSyncResponse(requestId, request.getTenantId(), request.getEdgeId(), false, "Sync process is active at the moment"));
        } else {
            log.trace("[{}][{}] Processing sync edge request [{}], serviceId [{}]", request.getTenantId(), request.getId(), request.getEdgeId(), request.getServiceId());
            localSyncEdgeRequests.put(requestId, responseConsumer);
            clusterService.pushEdgeSyncRequestToEdge(request);
            scheduleSyncRequestTimeout(request, requestId);
        }
    }

    private void onEdgeConnect(EdgeId edgeId, EdgeGrpcSessionManager edgeSession) {
        EdgeSessionState state = edgeSession.getState();
        Edge edge = state.getEdge();
        TenantId tenantId = state.getTenantId();
        log.info("[{}][{}] edge [{}] connected successfully.", tenantId, state.getSessionId(), edgeId);
        if (sessions.hasByEdgeId(edgeId)) {
            EdgeGrpcSessionManager existingSession = sessions.getByEdgeId(edgeId);
            if (existingSession != null) {
                UUID sessionId = existingSession.getState().getSessionId();
                log.info("[{}][{}] Replacing existing session [{}] for edge [{}]", tenantId, state.getSessionId(), sessionId, edgeId);
                existingSession.destroyAndMarkAsZombieIfFailed();
                sessions.removeBySessionId(sessionId);
            }
        }
        sessions.put(edgeSession);
        save(tenantId, edgeId, ACTIVITY_STATE, true);
        long lastConnectTs = System.currentTimeMillis();
        save(tenantId, edgeId, LAST_CONNECT_TIME, lastConnectTs);
        edgeIdServiceIdCache.put(edgeId, serviceInfoProvider.getServiceId());
        pushRuleEngineMessage(tenantId, edge, lastConnectTs, TbMsgType.CONNECT_EVENT);
        edgeSession.onEdgeConnect();
    }

    private void onEdgeDisconnect(Edge edge, UUID sessionId) {
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        log.info("[{}][{}] edge disconnected!", edgeId, sessionId);
        EdgeGrpcSessionManager current = sessions.getByEdgeId(edgeId);
        if (current != null && current.getState().getSessionId().equals(sessionId)) {
            EdgeGrpcSessionManager toRemove = sessions.removeByEdgeId(edgeId);
            toRemove.onEdgeDisconnect();
            toRemove.destroyAndMarkAsZombieIfFailed();
            sessions.removeBySessionId(sessionId);
            save(tenantId, edgeId, ACTIVITY_STATE, false);
            long lastDisconnectTs = System.currentTimeMillis();
            save(tenantId, edgeId, LAST_DISCONNECT_TIME, lastDisconnectTs);
            pushRuleEngineMessage(tenantId, edge, lastDisconnectTs, TbMsgType.DISCONNECT_EVENT);
        } else {
            log.info("[{}] edge session [{}] is not current anymore. Attempting to destroy it by sessionId.", edgeId, sessionId);
            EdgeGrpcSessionManager stale = sessions.removeBySessionId(sessionId);
            if (stale != null) {
                try {
                    stale.destroyAndMarkAsZombieIfFailed();
                    log.info("[{}][{}] Successfully destroyed stale session for edge [{}]", stale.getState().getTenantId(), sessionId, edgeId);
                } catch (Exception e) {
                    log.warn("[{}][{}] Failed to destroy stale session for edge [{}]", stale.getState().getTenantId(), sessionId, edgeId, e);
                }
            } else {
                log.debug("[{}] No session found by sessionId [{}] to destroy", edgeId, sessionId);
            }
        }
        edgeIdServiceIdCache.evict(edgeId);
    }

    private void scheduleSyncRequestTimeout(ToEdgeSyncRequest request, UUID requestId) {
        log.trace("[{}] scheduling sync edge request", requestId);
        executorService.schedule(() -> {
            log.trace("[{}] checking if sync edge request is not processed...", requestId);
            Consumer<FromEdgeSyncResponse> consumer = localSyncEdgeRequests.remove(requestId);
            if (consumer != null) {
                log.trace("[{}] timeout for processing sync edge request.", requestId);
                consumer.accept(new FromEdgeSyncResponse(requestId, request.getTenantId(), request.getEdgeId(), false, "Edge is not connected"));
            }
        }, 20, TimeUnit.SECONDS);
    }

    private void onEdgeHighPriorityEvent(EdgeHighPriorityMsg msg) {
        TenantId tenantId = msg.getTenantId();
        EdgeEvent edgeEvent = msg.getEdgeEvent();
        EdgeId edgeId = edgeEvent.getEdgeId();
        EdgeGrpcSessionManager session = sessions.getByEdgeId(edgeId);
        if (session != null && session.getState().isConnected()) {
            log.trace("[{}] onEdgeEvent [{}]", tenantId, edgeId);
            session.addEventToHighPriorityQueue(edgeEvent);
        }
    }

    private void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId) {
        EdgeGrpcSessionManager session = sessions.getByEdgeId(edgeId);
        if (session != null && session.getState().isConnected()) {
            log.trace("[{}] onEdgeEventUpdate [{}]", tenantId, edgeId.getId());
            session.onEdgeEventUpdate();
        }
    }

    private void startSyncProcess(TenantId tenantId, EdgeId edgeId, UUID requestId, String requestServiceId) {
        EdgeGrpcSessionManager session = sessions.getByEdgeId(edgeId);
        if (session != null) {
            EdgeSessionState sessionState = session.getState();
            if (sessionState.isSyncInProgress()) {
                clusterService.pushEdgeSyncResponseToCore(new FromEdgeSyncResponse(requestId, tenantId, edgeId, false, "Sync process is active at the moment"), requestServiceId);
            } else {
                boolean success = false;
                if (sessionState.isConnected()) {
                    session.startSyncProcess(true);
                    success = true;
                }
                clusterService.pushEdgeSyncResponseToCore(new FromEdgeSyncResponse(requestId, tenantId, edgeId, success, ""), requestServiceId);
            }
        }
    }

    private void processSyncResponse(FromEdgeSyncResponse response) {
        log.trace("[{}] Received response from sync service: [{}]", response.getId(), response);
        UUID requestId = response.getId();
        Consumer<FromEdgeSyncResponse> consumer = localSyncEdgeRequests.remove(requestId);
        if (consumer != null) {
            consumer.accept(response);
        } else {
            log.trace("[{}] Unknown or stale sync response received [{}]", requestId, response);
        }
    }

    private void pushRuleEngineMessage(TenantId tenantId, Edge edge, long ts, TbMsgType msgType) {
        try {
            EdgeId edgeId = edge.getId();
            ObjectNode edgeState = JacksonUtil.newObjectNode();
            boolean isConnected = TbMsgType.CONNECT_EVENT.equals(msgType);
            if (isConnected) {
                edgeState.put(ACTIVITY_STATE, true);
                edgeState.put(LAST_CONNECT_TIME, ts);
            } else {
                edgeState.put(ACTIVITY_STATE, false);
                edgeState.put(LAST_DISCONNECT_TIME, ts);
            }
            ctx.getRuleProcessor().process(EdgeConnectionTrigger.builder()
                    .tenantId(tenantId)
                    .customerId(edge.getCustomerId())
                    .edgeId(edgeId)
                    .edgeName(edge.getName())
                    .connected(isConnected).build());
            String data = JacksonUtil.toString(edgeState);
            TbMsgMetaData md = new TbMsgMetaData();
            if (!persistToTelemetry) {
                md.putValue(DataConstants.SCOPE, DataConstants.SERVER_SCOPE);
                md.putValue("edgeName", edge.getName());
                md.putValue("edgeType", edge.getType());
            }
            TbMsg tbMsg = TbMsg.newMsg()
                    .type(msgType)
                    .originator(edgeId)
                    .copyMetaData(md)
                    .dataType(TbMsgDataType.JSON)
                    .data(data)
                    .build();
            clusterService.pushMsgToRuleEngine(tenantId, edgeId, tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push {}", tenantId, edge.getId(), msgType, e);
        }
    }

    private void save(TenantId tenantId, EdgeId edgeId, String key, long value) {
        log.debug("[{}][{}] Updating long edge telemetry [{}] [{}]", tenantId, edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveTimeseries(TimeseriesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(edgeId)
                    .entry(new LongDataEntry(key, value))
                    .callback(new EdgeAttributeSaveCallback(tenantId, edgeId, key, value))
                    .build());
        } else {
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(edgeId)
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new LongDataEntry(key, value))
                    .callback(new EdgeAttributeSaveCallback(tenantId, edgeId, key, value))
                    .build());
        }
    }

    private void save(TenantId tenantId, EdgeId edgeId, String key, boolean value) {
        log.debug("[{}][{}] Updating boolean edge telemetry [{}] [{}]", tenantId, edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveTimeseries(TimeseriesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(edgeId)
                    .entry(new BooleanDataEntry(key, value))
                    .callback(new EdgeAttributeSaveCallback(tenantId, edgeId, key, value))
                    .build());
        } else {
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(edgeId)
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new BooleanDataEntry(key, value))
                    .callback(new EdgeAttributeSaveCallback(tenantId, edgeId, key, value))
                    .build());
        }
    }

    private void shutdownExecutorSafely(ExecutorService sendDownlinkExecutorService) {
        if (sendDownlinkExecutorService != null && !sendDownlinkExecutorService.isShutdown()) {
            sendDownlinkExecutorService.shutdown();
        }
    }
}
