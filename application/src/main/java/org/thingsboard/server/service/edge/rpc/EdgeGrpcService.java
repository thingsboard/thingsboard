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
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.gen.edge.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.ResponseMsg;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges.rpc", value = "enabled", havingValue = "true")
public class EdgeGrpcService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase implements EdgeRpcService {

    private final Map<EdgeId, EdgeGrpcSession> sessions = new ConcurrentHashMap<>();
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

    @Autowired
    private EdgeContextComponent ctx;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

    private Server server;

    private ExecutorService executor;

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
        log.info("Edge RPC service initialized!");
        executor = Executors.newSingleThreadExecutor();
        processHandleMessages();
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
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
            log.debug("Closing and removing session for edge [{}]", edgeId);
            session.close();
            sessions.remove(edgeId);
        }
    }

    private void onEdgeConnect(EdgeId edgeId, EdgeGrpcSession edgeGrpcSession) {
        log.debug("[{}] onEdgeConnect [{}]", edgeId, edgeGrpcSession.getSessionId());
        sessions.put(edgeId, edgeGrpcSession);
        save(edgeId, DefaultDeviceStateService.ACTIVITY_STATE, true);
        save(edgeId, DefaultDeviceStateService.LAST_CONNECT_TIME, System.currentTimeMillis());
    }

    public EdgeGrpcSession getEdgeGrpcSessionById(EdgeId edgeId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            return session;
        } else {
            throw new RuntimeException("Edge is not connected");
        }
    }

    private void processHandleMessages() {
        executor.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    if (sessions.size() > 0) {
                        for (EdgeGrpcSession session : sessions.values()) {
                            session.processHandleMessages();
                        }
                    } else {
                        log.trace("No sessions available, sleep for the next run");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignore) {}
                    }
                } catch (Exception e) {
                    log.warn("Failed to process messages handling!", e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {}
                }
            }
        });
    }

    private void onEdgeDisconnect(EdgeId edgeId) {
        log.debug("[{}] onEdgeDisconnect", edgeId);
        sessions.remove(edgeId);
        save(edgeId, DefaultDeviceStateService.ACTIVITY_STATE, false);
        save(edgeId, DefaultDeviceStateService.LAST_DISCONNECT_TIME, System.currentTimeMillis());
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
