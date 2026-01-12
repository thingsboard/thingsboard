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
package org.thingsboard.server.service.edge.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.thingsboard.server.common.data.ResourceUtils;
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
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_CONNECT_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_DISCONNECT_TIME;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class EdgeGrpcService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase implements EdgeRpcService {

    private final ConcurrentMap<EdgeId, EdgeGrpcSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, EdgeGrpcSession> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<EdgeId, Lock> sessionNewEventsLocks = new ConcurrentHashMap<>();
    private final Map<EdgeId, Boolean> sessionNewEvents = new HashMap<>();
    private final ConcurrentMap<EdgeId, ScheduledFuture<?>> sessionEdgeEventChecks = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Consumer<FromEdgeSyncResponse>> localSyncEdgeRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<EdgeId, Boolean> edgeEventsMigrationProcessed = new ConcurrentHashMap<>();
    private final List<EdgeGrpcSession> zombieSessions = new ArrayList<>();

    @Value("${edges.rpc.port}")
    private int rpcPort;
    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${edges.rpc.ssl.cert}")
    private String certFileResource;
    @Value("${edges.rpc.ssl.private_key}")
    private String privateKeyResource;
    @Value("${edges.state.persistToTelemetry:false}")
    private boolean persistToTelemetry;
    @Value("${edges.rpc.client_max_keep_alive_time_sec:1}")
    private int clientMaxKeepAliveTimeSec;
    @Value("${edges.rpc.max_inbound_message_size:4194304}")
    private int maxInboundMessageSize;
    @Value("${edges.rpc.keep_alive_time_sec:10}")
    private int keepAliveTimeSec;
    @Value("${edges.rpc.keep_alive_timeout_sec:5}")
    private int keepAliveTimeoutSec;
    @Value("${edges.scheduler_pool_size}")
    private int schedulerPoolSize;

    @Value("${edges.send_scheduler_pool_size}")
    private int sendSchedulerPoolSize;

    @Value("${edges.max_high_priority_queue_size_per_session:10000}")
    private int maxHighPriorityQueueSizePerSession;

    @Autowired
    @Lazy
    private EdgeContextComponent ctx;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private TbTransactionalCache<EdgeId, String> edgeIdServiceIdCache;

    @Autowired
    private TopicService topicService;

    @Autowired
    private TbCoreQueueFactory tbCoreQueueFactory;

    @Autowired
    private Optional<KafkaAdmin> kafkaAdmin;

    private Server server;

    private ScheduledExecutorService edgeEventProcessingExecutorService;

    private ScheduledExecutorService sendDownlinkExecutorService;

    private ScheduledExecutorService executorService;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onStartUp() {
        log.info("Initializing Edge RPC service!");
        NettyServerBuilder builder = NettyServerBuilder.forPort(rpcPort)
                .permitKeepAliveTime(clientMaxKeepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTime(keepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTimeout(keepAliveTimeoutSec, TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true)
                .maxInboundMessageSize(maxInboundMessageSize)
                .addService(this);
        if (sslEnabled) {
            try {
                InputStream certFileIs = ResourceUtils.getInputStream(this, certFileResource);
                InputStream privateKeyFileIs = ResourceUtils.getInputStream(this, privateKeyResource);
                builder.useTransportSecurity(certFileIs, privateKeyFileIs);
            } catch (Exception e) {
                log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
                throw new RuntimeException("Unable to set up SSL context!", e);
            }
        }
        server = builder.build();
        log.info("Going to start Edge RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start Edge RPC server!", e);
            throw new RuntimeException("Failed to start Edge RPC server!");
        }
        this.edgeEventProcessingExecutorService = ThingsBoardExecutors.newScheduledThreadPool(schedulerPoolSize, "edge-event-check-scheduler");
        this.sendDownlinkExecutorService = ThingsBoardExecutors.newScheduledThreadPool(sendSchedulerPoolSize, "edge-send-scheduler");
        this.executorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("edge-service");
        this.executorService.scheduleAtFixedRate(this::cleanupZombieSessions, 60, 60, TimeUnit.SECONDS);
        log.info("Edge RPC service initialized!");
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
        for (Map.Entry<EdgeId, ScheduledFuture<?>> entry : sessionEdgeEventChecks.entrySet()) {
            EdgeId edgeId = entry.getKey();
            ScheduledFuture<?> sessionEdgeEventCheck = entry.getValue();
            if (sessionEdgeEventCheck != null && !sessionEdgeEventCheck.isCancelled() && !sessionEdgeEventCheck.isDone()) {
                sessionEdgeEventCheck.cancel(true);
                sessionEdgeEventChecks.remove(edgeId);
            }
        }
        if (edgeEventProcessingExecutorService != null) {
            edgeEventProcessingExecutorService.shutdownNow();
        }
        if (sendDownlinkExecutorService != null) {
            sendDownlinkExecutorService.shutdownNow();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> outputStream) {
        EdgeGrpcSession session = createEdgeGrpcSession(outputStream);
        return session.getInputStream();
    }

    private EdgeGrpcSession createEdgeGrpcSession(StreamObserver<ResponseMsg> outputStream) {
        return kafkaAdmin.isPresent()
                ? new KafkaEdgeGrpcSession(ctx, topicService, tbCoreQueueFactory, kafkaAdmin.get(), outputStream, this::onEdgeConnect, this::onEdgeDisconnect,
                sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession)
                : new PostgresEdgeGrpcSession(ctx, outputStream, this::onEdgeConnect, this::onEdgeDisconnect,
                sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
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
        EdgeGrpcSession session = sessions.get(edge.getId());
        if (session != null && session.isConnected()) {
            log.debug("[{}] Updating configuration for edge [{}] [{}]", tenantId, edge.getName(), edge.getId());
            session.onConfigurationUpdate(edge);
        } else {
            log.debug("[{}] Session doesn't exist for edge [{}] [{}]", tenantId, edge.getName(), edge.getId());
        }
    }

    @Override
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            log.info("[{}] Closing and removing session for edge [{}]", tenantId, edgeId);
            destroySession(session);
            session.cleanUp();
            sessions.remove(edgeId);
            sessionsById.remove(session.getSessionId());
            final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
            newEventLock.lock();
            try {
                sessionNewEvents.remove(edgeId);
            } finally {
                newEventLock.unlock();
            }
            cancelScheduleEdgeEventsCheck(edgeId);
        }
    }

    private void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            log.trace("[{}] onEdgeEventUpdate [{}]", tenantId, edgeId.getId());
            updateSessionEventsFlag(tenantId, edgeId);
        }
    }

    private void onEdgeHighPriorityEvent(EdgeHighPriorityMsg msg) {
        TenantId tenantId = msg.getTenantId();
        EdgeEvent edgeEvent = msg.getEdgeEvent();
        EdgeId edgeId = edgeEvent.getEdgeId();
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            log.trace("[{}] onEdgeEvent [{}]", tenantId, edgeId);
            session.addEventToHighPriorityQueue(edgeEvent);
            updateSessionEventsFlag(tenantId, edgeId);
        }
    }

    private void updateSessionEventsFlag(TenantId tenantId, EdgeId edgeId) {
        final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
        newEventLock.lock();
        try {
            if (Boolean.FALSE.equals(sessionNewEvents.get(edgeId))) {
                log.trace("[{}] set session new events flag to true [{}]", tenantId, edgeId.getId());
                sessionNewEvents.put(edgeId, true);
            }
        } finally {
            newEventLock.unlock();
        }
    }

    private void onEdgeConnect(EdgeId edgeId, EdgeGrpcSession edgeGrpcSession) {
        Edge edge = edgeGrpcSession.getEdge();
        TenantId tenantId = edge.getTenantId();
        log.info("[{}][{}] edge [{}] connected successfully.", tenantId, edgeGrpcSession.getSessionId(), edgeId);
        if (sessions.containsKey(edgeId)) {
            EdgeGrpcSession existing = sessions.get(edgeId);
            if (existing != null) {
                log.info("[{}][{}] Replacing existing session [{}] for edge [{}]", tenantId, edgeGrpcSession.getSessionId(), existing.getSessionId(), edgeId);
                destroySession(existing);
                sessionsById.remove(existing.getSessionId());
            }
        }
        sessions.put(edgeId, edgeGrpcSession);
        sessionsById.put(edgeGrpcSession.getSessionId(), edgeGrpcSession);
        final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
        newEventLock.lock();
        try {
            sessionNewEvents.put(edgeId, true);
        } finally {
            newEventLock.unlock();
        }
        save(tenantId, edgeId, ACTIVITY_STATE, true);
        long lastConnectTs = System.currentTimeMillis();
        save(tenantId, edgeId, LAST_CONNECT_TIME, lastConnectTs);
        edgeIdServiceIdCache.put(edgeId, serviceInfoProvider.getServiceId());
        pushRuleEngineMessage(tenantId, edge, lastConnectTs, TbMsgType.CONNECT_EVENT);
        cancelScheduleEdgeEventsCheck(edgeId);
        edgeEventsMigrationProcessed.putIfAbsent(edgeId, Boolean.FALSE);
        scheduleEdgeEventsCheck(edgeGrpcSession);
    }

    private void startSyncProcess(TenantId tenantId, EdgeId edgeId, UUID requestId, String requestServiceId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null) {
            if (session.isSyncInProgress()) {
                clusterService.pushEdgeSyncResponseToCore(new FromEdgeSyncResponse(requestId, tenantId, edgeId, false, "Sync process is active at the moment"), requestServiceId);
            } else {
                boolean success = false;
                if (session.isConnected()) {
                    session.startSyncProcess(true);
                    success = true;
                }
                clusterService.pushEdgeSyncResponseToCore(new FromEdgeSyncResponse(requestId, tenantId, edgeId, success, ""), requestServiceId);
            }
        }
    }

    @Override
    public void processSyncRequest(TenantId tenantId, EdgeId edgeId, Consumer<FromEdgeSyncResponse> responseConsumer) {
        ToEdgeSyncRequest request = new ToEdgeSyncRequest(UUID.randomUUID(), tenantId, edgeId, serviceInfoProvider.getServiceId());

        UUID requestId = request.getId();
        EdgeGrpcSession session = sessions.get(request.getEdgeId());
        if (session != null && session.isSyncInProgress()) {
            responseConsumer.accept(new FromEdgeSyncResponse(requestId, request.getTenantId(), request.getEdgeId(), false, "Sync process is active at the moment"));
        } else {
            log.trace("[{}][{}] Processing sync edge request [{}], serviceId [{}]", request.getTenantId(), request.getId(), request.getEdgeId(), request.getServiceId());
            localSyncEdgeRequests.put(requestId, responseConsumer);
            clusterService.pushEdgeSyncRequestToEdge(request);
            scheduleSyncRequestTimeout(request, requestId);
        }
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

    private void scheduleEdgeEventsCheck(EdgeGrpcSession session) {
        EdgeId edgeId = session.getEdge().getId();
        TenantId tenantId = session.getEdge().getTenantId();

        cancelScheduleEdgeEventsCheck(edgeId);

        if (sessions.containsKey(edgeId)) {
            ScheduledFuture<?> edgeEventCheckTask = edgeEventProcessingExecutorService.schedule(() -> {
                try {
                    final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
                    newEventLock.lock();
                    try {
                        if (Boolean.TRUE.equals(sessionNewEvents.get(edgeId))) {
                            log.trace("[{}][{}] set session new events flag to false", tenantId, edgeId.getId());
                            sessionNewEvents.put(edgeId, false);
                            session.processHighPriorityEvents();
                            processEdgeEventMigrationIfNeeded(session, edgeId);
                            if (Boolean.TRUE.equals(edgeEventsMigrationProcessed.get(edgeId))) {
                                Futures.addCallback(session.processEdgeEvents(), new FutureCallback<>() {
                                    @Override
                                    public void onSuccess(Boolean newEventsAdded) {
                                        if (Boolean.TRUE.equals(newEventsAdded)) {
                                            log.trace("[{}][{}] new events added. set session new events flag to true", tenantId, edgeId.getId());
                                            sessionNewEvents.put(edgeId, true);
                                        }
                                        scheduleEdgeEventsCheck(session);
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, session.getEdge().getId().getId(), t);
                                        scheduleEdgeEventsCheck(session);
                                    }
                                }, ctx.getGrpcCallbackExecutorService());
                            } else {
                                scheduleEdgeEventsCheck(session);
                            }
                        } else {
                            scheduleEdgeEventsCheck(session);
                        }
                    } finally {
                        newEventLock.unlock();
                    }
                } catch (Exception e) {
                    log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, session.getEdge().getId().getId(), e);
                }
            }, ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval(), TimeUnit.MILLISECONDS);
            sessionEdgeEventChecks.put(edgeId, edgeEventCheckTask);
            log.trace("[{}] Check edge event scheduled for edge [{}]", tenantId, edgeId.getId());
        } else {
            log.debug("[{}] Session was removed and edge event check schedule must not be started [{}]",
                    tenantId, edgeId.getId());
        }
    }

    private void processEdgeEventMigrationIfNeeded(EdgeGrpcSession session, EdgeId edgeId) throws Exception {
        boolean isMigrationProcessed = edgeEventsMigrationProcessed.getOrDefault(edgeId, Boolean.FALSE);
        if (!isMigrationProcessed) {
            log.info("Starting edge event migration for edge [{}]", edgeId.getId());
            Boolean eventsExist = session.migrateEdgeEvents().get();
            if (Boolean.TRUE.equals(eventsExist)) {
                log.info("Migration still in progress for edge [{}]", edgeId.getId());
                sessionNewEvents.put(edgeId, true);
                scheduleEdgeEventsCheck(session);
            } else if (Boolean.FALSE.equals(eventsExist)) {
                log.info("Migration completed for edge [{}]", edgeId.getId());
                edgeEventsMigrationProcessed.put(edgeId, true);
            }
        }
    }

    private void cancelScheduleEdgeEventsCheck(EdgeId edgeId) {
        log.trace("[{}] cancelling edge event check for edge", edgeId);
        if (sessionEdgeEventChecks.containsKey(edgeId)) {
            ScheduledFuture<?> sessionEdgeEventCheck = sessionEdgeEventChecks.get(edgeId);
            if (sessionEdgeEventCheck != null && !sessionEdgeEventCheck.isCancelled() && !sessionEdgeEventCheck.isDone()) {
                sessionEdgeEventCheck.cancel(true);
                sessionEdgeEventChecks.remove(edgeId);
            }
        }
    }

    private void onEdgeDisconnect(Edge edge, UUID sessionId) {
        EdgeId edgeId = edge.getId();
        log.info("[{}][{}] edge disconnected!", edgeId, sessionId);
        EdgeGrpcSession current = sessions.get(edgeId);
        if (current != null && current.getSessionId().equals(sessionId)) {
            EdgeGrpcSession toRemove = sessions.remove(edgeId);
            final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
            newEventLock.lock();
            try {
                sessionNewEvents.remove(edgeId);
            } finally {
                newEventLock.unlock();
            }
            destroySession(toRemove);
            sessionsById.remove(sessionId);
            TenantId tenantId = toRemove.getEdge().getTenantId();
            save(tenantId, edgeId, ACTIVITY_STATE, false);
            long lastDisconnectTs = System.currentTimeMillis();
            save(tenantId, edgeId, LAST_DISCONNECT_TIME, lastDisconnectTs);
            pushRuleEngineMessage(toRemove.getEdge().getTenantId(), edge, lastDisconnectTs, TbMsgType.DISCONNECT_EVENT);
            cancelScheduleEdgeEventsCheck(edgeId);
        } else {
            log.info("[{}] edge session [{}] is not current anymore. Attempting to destroy it by sessionId.", edgeId, sessionId);
            EdgeGrpcSession stale = sessionsById.remove(sessionId);
            if (stale != null) {
                try {
                    destroySession(stale);
                    log.info("[{}][{}] Successfully destroyed stale session for edge [{}]", stale.getTenantId(), sessionId, edgeId);
                } catch (Exception e) {
                    log.warn("[{}][{}] Failed to destroy stale session for edge [{}]", stale.getTenantId(), sessionId, edgeId, e);
                }
            } else {
                log.debug("[{}] No session found by sessionId [{}] to destroy", edgeId, sessionId);
            }
        }
        edgeIdServiceIdCache.evict(edgeId);
    }

    private void destroySession(EdgeGrpcSession session) {
        try (session) {
            if (!session.destroy()) {
                log.warn("[{}][{}] Session destroy failed for edge [{}] with session id [{}]. Adding to zombie queue for later cleanup.",
                        session.getTenantId(), session.getEdge().getId(), session.getEdge().getName(), session.getSessionId());
                zombieSessions.add(session);
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Exception during session destroy for edge [{}] with session id [{}]",
                    session.getTenantId(), session.getEdge().getId(), session.getEdge().getName(), session.getSessionId(), e);
        }
    }

    private void save(TenantId tenantId, EdgeId edgeId, String key, long value) {
        log.debug("[{}][{}] Updating long edge telemetry [{}] [{}]", tenantId, edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveTimeseries(TimeseriesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(edgeId)
                    .entry(new LongDataEntry(key, value))
                    .callback(new AttributeSaveCallback(tenantId, edgeId, key, value))
                    .build());
        } else {
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(edgeId)
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new LongDataEntry(key, value))
                    .callback(new AttributeSaveCallback(tenantId, edgeId, key, value))
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
                    .callback(new AttributeSaveCallback(tenantId, edgeId, key, value))
                    .build());
        } else {
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(edgeId)
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new BooleanDataEntry(key, value))
                    .callback(new AttributeSaveCallback(tenantId, edgeId, key, value))
                    .build());
        }
    }

    private static class AttributeSaveCallback implements FutureCallback<Void> {

        private final TenantId tenantId;
        private final EdgeId edgeId;
        private final String key;
        private final Object value;

        AttributeSaveCallback(TenantId tenantId, EdgeId edgeId, String key, Object value) {
            this.tenantId = tenantId;
            this.edgeId = edgeId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@Nullable Void result) {
            log.trace("[{}][{}] Successfully updated attribute [{}] with value [{}]", tenantId, edgeId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}][{}] Failed to update attribute [{}] with value [{}]", tenantId, edgeId, key, value, t);
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

    private void cleanupZombieSessions() {
        try {
            tryToDestroyZombieSessions(getZombieSessions(sessions.values()), s -> sessions.remove(s.getEdge().getId()));
            tryToDestroyZombieSessions(getZombieSessions(sessionsById.values()), s -> sessionsById.remove(s.getSessionId()));

            zombieSessions.removeIf(zombie -> {
                if (zombie.destroy()) {
                    log.info("[{}][{}] Successfully cleaned up zombie session [{}] for edge [{}].",
                            zombie.getTenantId(), zombie.getEdge().getId(), zombie.getSessionId(), zombie.getEdge().getName());
                    return true;
                } else {
                    log.warn("[{}][{}] Failed to remove zombie session [{}] for edge [{}].",
                            zombie.getTenantId(), zombie.getEdge().getId(), zombie.getSessionId(), zombie.getEdge().getName());
                    return false;
                }
            });
        } catch (Exception e) {
            log.warn("Failed to cleanup kafka sessions", e);
        }
    }

    private List<EdgeGrpcSession> getZombieSessions(Collection<EdgeGrpcSession> sessions) {
        List<EdgeGrpcSession> result = new ArrayList<>();
        for (EdgeGrpcSession session : sessions) {
            if (isKafkaSessionAndZombie(session)) {
                result.add(session);
            }
        }
        return result;
    }

    private void tryToDestroyZombieSessions(List<EdgeGrpcSession> sessionsToRemove, Function<EdgeGrpcSession, EdgeGrpcSession> removeFunc) {
        for (EdgeGrpcSession toRemove : sessionsToRemove) {
            log.info("[{}] Destroying session for edge because edge is not connected", toRemove.getEdge().getId());
            if (toRemove.destroy()) {
                removeFunc.apply(toRemove);
            }
        }
    }

    private boolean isKafkaSessionAndZombie(EdgeGrpcSession session) {
        if (session instanceof KafkaEdgeGrpcSession kafkaSession) {
            log.debug("[{}] kafkaSession.isConnected() = {}, kafkaSession.getConsumer().getConsumer().isStopped() = {}",
                    kafkaSession.getEdge().getId(),
                    kafkaSession.isConnected(),
                    kafkaSession.getConsumer() != null ? kafkaSession.getConsumer().getConsumer() != null ? kafkaSession.getConsumer().getConsumer().isStopped() : null : null);
            return !kafkaSession.isConnected() &&
                    kafkaSession.getConsumer() != null &&
                    kafkaSession.getConsumer().getConsumer() != null &&
                    !kafkaSession.getConsumer().getConsumer().isStopped();
        }
        return false;

    }

}
