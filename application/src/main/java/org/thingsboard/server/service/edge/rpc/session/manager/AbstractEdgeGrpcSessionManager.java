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
package org.thingsboard.server.service.edge.rpc.session.manager;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.gen.edge.v1.EdgeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.DownlinkMessageMapper;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.EdgeUplinkMessageDispatcher;
import org.thingsboard.server.service.edge.rpc.session.EdgeGrpcSession;
import org.thingsboard.server.service.edge.rpc.session.EdgeGrpcSessionDelegate;
import org.thingsboard.server.service.edge.rpc.session.EdgeSession;
import org.thingsboard.server.service.edge.rpc.session.ZombieSessionCleanupService;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

@Slf4j
public abstract class AbstractEdgeGrpcSessionManager extends EdgeGrpcSessionDelegate implements EdgeGrpcSessionManager {

    @Value("${edges.max_high_priority_queue_size_per_session:10000}")
    protected int maxHighPriorityQueueSizePerSession;

    @Value("${edges.rpc.max_inbound_message_size:4194304}")
    protected int maxInboundMessageSize;

    @Autowired
    protected ZombieSessionCleanupService zombieSessionCleanupService;

    @Autowired
    @Lazy
    protected EdgeContextComponent ctx;

    @Autowired
    protected DownlinkMessageMapper downlinkMessageMapper;

    @Autowired
    protected EdgeUplinkMessageDispatcher uplinkMessageDispatcher;

    @Getter
    protected EdgeSession session;

    private final Lock initLock = new ReentrantLock();

    @Override
    public StreamObserver<RequestMsg> initInputStream(StreamObserver<ResponseMsg> outputStream,
                                                      BiConsumer<EdgeId, EdgeGrpcSessionManager> sessionOpenListener,
                                                      BiConsumer<Edge, UUID> sessionCloseListener,
                                                      ScheduledExecutorService sendDownlinkExecutorService) {
        if (session != null) {
            throw new IllegalStateException("Session already initialized");
        }
        initLock.lock();
        try {
            this.session = new EdgeGrpcSession(
                    this, ctx, outputStream, downlinkMessageMapper, uplinkMessageDispatcher,
                    sessionOpenListener, sessionCloseListener,
                    sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
            return session.initInputStream();
        } finally {
            initLock.unlock();
        }
    }

    @Override
    public EdgeSessionState getState() {
        if (session == null) {
            return null;
        }
        return session.getState();
    }

    @Override
    public void onConfigurationUpdate(Edge edge) {
        EdgeUpdateMsg edgeConfig = EdgeUpdateMsg.newBuilder()
                .setConfiguration(EdgeMsgConstructorUtils.constructEdgeConfiguration(edge)).build();

        ResponseMsg edgeConfigMsg = ResponseMsg.newBuilder()
                .setEdgeUpdateMsg(edgeConfig)
                .build();

        session.sendDownlinkMsg(edgeConfigMsg);
    }

    @Override
    public void destroyAndMarkAsZombieIfFailed() {
        EdgeSessionState state = getState();
        final var finalSession = session;

        try (finalSession) {
            if (!destroy()) {
                log.warn("[{}][{}] Session destroy failed for edge [{}] with session id [{}]. Adding to zombie queue for later cleanup.",
                        state.getTenantId(), state.getEdgeId(), state.getEdge().getName(), state.getSessionId());
                zombieSessionCleanupService.add(this);
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Exception during session destroy for edge [{}] with session id [{}]",
                    state.getTenantId(), state.getEdgeId(), state.getEdge().getName(), state.getSessionId(), e);
        }
    }
}
