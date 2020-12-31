/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.FutureCallback;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.gen.edge.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class EdgeGrpcService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase implements EdgeRpcService {

    private final ConcurrentMap<EdgeId, EdgeGrpcSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<EdgeId, Boolean> sessionNewEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<EdgeId, ScheduledFuture<?>> sessionEdgeEventChecks = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

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
    @Value("${edges.rpc.client_max_keep_alive_time_sec}")
    private int clientMaxKeepAliveTimeSec;

    @Value("${edges.scheduler_pool_size}")
    private int schedulerPoolSize;

    @Autowired
    private EdgeContextComponent ctx;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

    private Server server;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        log.info("Initializing Edge RPC service!");
        NettyServerBuilder builder = NettyServerBuilder.forPort(rpcPort)
                .permitKeepAliveTime(clientMaxKeepAliveTimeSec, TimeUnit.SECONDS)
                .addService(this);
        if (sslEnabled) {
            try {
                File certFile = new File(Resources.getResource(certFileResource).toURI());
                File privateKeyFile = new File(Resources.getResource(privateKeyResource).toURI());
                builder.useTransportSecurity(certFile, privateKeyFile);
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
        this.scheduler = Executors.newScheduledThreadPool(schedulerPoolSize, ThingsBoardThreadFactory.forName("edge-scheduler"));
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
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> outputStream) {
        return new EdgeGrpcSession(ctx, outputStream, this::onEdgeConnect, this::onEdgeDisconnect, mapper).getInputStream();
    }

    @Override
    public void updateEdge(Edge edge) {
        EdgeGrpcSession session = sessions.get(edge.getId());
        if (session != null && session.isConnected()) {
            log.debug("[{}] Updating configuration for edge [{}] [{}]", edge.getTenantId(), edge.getName(), edge.getId());
            session.onConfigurationUpdate(edge);
        } else {
            log.warn("[{}] Session doesn't exist for edge [{}] [{}]", edge.getTenantId(), edge.getName(), edge.getId());
        }
    }

    @Override
    public void deleteEdge(EdgeId edgeId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            log.info("Closing and removing session for edge [{}]", edgeId);
            session.close();
            sessions.remove(edgeId);
            sessionNewEvents.remove(edgeId);
            cancelScheduleEdgeEventsCheck(edgeId);
        }
    }

    @Override
    public void onEdgeEvent(EdgeId edgeId) {
        log.trace("[{}] onEdgeEvent", edgeId.getId());
        if (!sessionNewEvents.get(edgeId)) {
            log.trace("[{}] set session new events flag to true", edgeId.getId());
            sessionNewEvents.put(edgeId, true);
        }
    }

    private void onEdgeConnect(EdgeId edgeId, EdgeGrpcSession edgeGrpcSession) {
        log.info("[{}] edge [{}] connected successfully.", edgeGrpcSession.getSessionId(), edgeId);
        sessions.put(edgeId, edgeGrpcSession);
        sessionNewEvents.put(edgeId, false);
        save(edgeId, DefaultDeviceStateService.ACTIVITY_STATE, true);
        save(edgeId, DefaultDeviceStateService.LAST_CONNECT_TIME, System.currentTimeMillis());
        cancelScheduleEdgeEventsCheck(edgeId);
        scheduleEdgeEventsCheck(edgeGrpcSession);
    }

    public EdgeGrpcSession getEdgeGrpcSessionById(TenantId tenantId, EdgeId edgeId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            return session;
        } else {
            log.error("[{}] Edge is not connected [{}]", tenantId, edgeId);
            throw new RuntimeException("Edge is not connected");
        }
    }

    private void scheduleEdgeEventsCheck(EdgeGrpcSession session) {
        EdgeId edgeId = session.getEdge().getId();
        UUID tenantId = session.getEdge().getTenantId().getId();
        if (sessions.containsKey(edgeId)) {
            ScheduledFuture<?> schedule = scheduler.schedule(() -> {
                try {
                    if (sessionNewEvents.get(edgeId)) {
                        log.trace("[{}] Set session new events flag to false", edgeId.getId());
                        sessionNewEvents.put(edgeId, false);
                        session.processEdgeEvents();
                    }
                } catch (Exception e) {
                    log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, session.getEdge().getId().getId(), e);
                }
                scheduleEdgeEventsCheck(session);
            }, ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval(), TimeUnit.MILLISECONDS);
            sessionEdgeEventChecks.put(edgeId, schedule);
            log.trace("[{}] Check edge event scheduled for edge [{}]", tenantId, edgeId.getId());
        } else {
            log.debug("[{}] Session was removed and edge event check schedule must not be started [{}]",
                    tenantId, edgeId.getId());
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

    private void onEdgeDisconnect(EdgeId edgeId) {
        log.info("[{}] edge disconnected!", edgeId);
        sessions.remove(edgeId);
        sessionNewEvents.remove(edgeId);
        save(edgeId, DefaultDeviceStateService.ACTIVITY_STATE, false);
        save(edgeId, DefaultDeviceStateService.LAST_DISCONNECT_TIME, System.currentTimeMillis());
        cancelScheduleEdgeEventsCheck(edgeId);
    }

    private void save(EdgeId edgeId, String key, long value) {
        log.debug("[{}] Updating long edge telemetry [{}] [{}]", edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    TenantId.SYS_TENANT_ID, edgeId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry(key, value))),
                    new AttributeSaveCallback(edgeId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, edgeId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(edgeId, key, value));
        }
    }

    private void save(EdgeId edgeId, String key, boolean value) {
        log.debug("[{}] Updating boolean edge telemetry [{}] [{}]", edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    TenantId.SYS_TENANT_ID, edgeId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))),
                    new AttributeSaveCallback(edgeId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, edgeId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(edgeId, key, value));
        }
    }

    private static class AttributeSaveCallback implements FutureCallback<Void> {
        private final EdgeId edgeId;
        private final String key;
        private final Object value;

        AttributeSaveCallback(EdgeId edgeId, String key, Object value) {
            this.edgeId = edgeId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@Nullable Void result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", edgeId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", edgeId, key, value, t);
        }
    }
}
