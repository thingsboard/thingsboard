// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.agent.v1.AgentRpcServiceGrpc;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.gen.agent.v1.ProvisionRequest;
import org.thingsboard.server.gen.agent.v1.ProvisionResponse;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.agent.v1.StartLogStream;
import org.thingsboard.server.gen.agent.v1.StopLogStream;
import org.thingsboard.server.gen.transport.TransportProtos.LogStreamRequestProto;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;
import org.thingsboard.server.service.agent.msg.inbound.AgentInboundExecutor;
import org.thingsboard.server.service.agent.msg.inbound.AgentInboundMessageDispatcher;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;
import org.thingsboard.server.service.agent.session.BaseAgentSession;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agents", value = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentGrpcService extends AgentRpcServiceGrpc.AgentRpcServiceImplBase implements AgentRpcService {

    private static final long MAX_REPLAY_LOOKBACK_MS = TimeUnit.MINUTES.toMillis(2);
    private static final String PROVISION_FAILURE_MESSAGE = "Failed to provision agent";

    @Value("${agents.write_pool_size:8}")
    private int writePoolSize;

    @Value("${agents.max_pending_messages_per_session:10000}")
    private int maxPendingMessagesPerSession;

    private final LogChunkBuffer logChunkBuffer;
    private final AgentInboundMessageDispatcher inboundMessageDispatcher;
    private final AgentInboundExecutor inboundExecutor;
    private final AgentSessionService agentSessionService;
    private final AgentSessionRegistry sessions;
    private final AgentProvisionService agentProvisionService;
    private final AgentEventProcessor agentEventProcessor;
    private final AgentContextComponent agentCtx;

    private ListeningExecutorService writer;

    @PostConstruct
    private void init() {
        this.writer = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool(writePoolSize, ThingsBoardThreadFactory.forName("agent-writer")));
    }

    @PreDestroy
    private void destroy() {
        if (writer != null && !writer.isShutdown()) {
            writer.shutdown();
        }
    }

    @Override
    public StreamObserver<AgentToServer> controlStream(StreamObserver<ServerToAgent> responseObserver) {
        if (!(responseObserver instanceof ServerCallStreamObserver<ServerToAgent> serverCallStreamObserver)) {
            throw new IllegalStateException("Expected ServerCallStreamObserver but got: "
                    + (responseObserver == null ? "null" : responseObserver.getClass().getName()));
        }

        AtomicReference<AgentSession> sessionRef = new AtomicReference<>();
        StreamObserver<AgentToServer> requestObserver = getAgentToServerStreamObserver(serverCallStreamObserver, sessionRef);

        serverCallStreamObserver.setOnReadyHandler(() -> {
            AgentSession session = sessionRef.get();
            if (session instanceof BaseAgentSession s) {
                s.drainIfPossible();
            }
        });
        // grpc-java guarantees exactly one of the two runs when the RPC terminates; without the close handler a
        // server-initiated close leaves the session registered and the agent reads as connected
        serverCallStreamObserver.setOnCancelHandler(() -> deregisterSession(sessionRef, agentSessionService::onError));
        serverCallStreamObserver.setOnCloseHandler(() -> deregisterSession(sessionRef, agentSessionService::onCompleted));

        return requestObserver;
    }

    private StreamObserver<AgentToServer> getAgentToServerStreamObserver(
            ServerCallStreamObserver<ServerToAgent> serverCallStreamObserver,
            AtomicReference<AgentSession> sessionRef) {

        AtomicBoolean initializing = new AtomicBoolean(false);

        return new StreamObserver<>() {
            @Override
            public void onNext(AgentToServer msg) {
                AgentSession session = sessionRef.get();
                if (session != null) {
                    processInboundMessage(session, msg);
                    return;
                }
                if (initializing.compareAndSet(false, true)) {
                    tryInitSession(msg, serverCallStreamObserver, sessionRef, initializing);
                } else {
                    log.trace("Dropping message while session is initializing");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                AgentSession session = sessionRef.get();
                if (session == null) {
                    log.trace("Ignoring onError for uninitialized session");
                    return;
                }
                agentSessionService.onError(session);
                session.closeSilently();
            }

            @Override
            public void onCompleted() {
                AgentSession session = sessionRef.get();
                if (session == null) {
                    log.trace("Ignoring onCompleted for uninitialized session");
                    return;
                }
                agentSessionService.onCompleted(session);
                session.complete();
            }
        };
    }


    private void deregisterSession(AtomicReference<AgentSession> sessionRef, Consumer<AgentSession> notifySessionService) {
        AgentSession session = sessionRef.get();
        if (session != null) {
            notifySessionService.accept(session);
            session.closeSilently();
        }
    }

    @Override
    public boolean push(AgentId agentId, ServerToAgent msg) throws AgentSessionNotFoundException {
        AgentSession session = sessions.getByAgentId(agentId);
        if (session == null) {
            throw new AgentSessionNotFoundException(agentId);
        }
        return session.push(msg);
    }

    @Override
    public void processLogStreamRequest(LogStreamRequestProto req) {
        AgentId agentId = AgentId.fromMsbAndLsb(req.getAgentIdMSB(), req.getAgentIdLSB());
        if (req.getStop()) {
            stopLogStream(agentId, req.getProjectName(), req.getUnitIdentifier());
            return;
        }
        var tenantId = TenantId.fromUUID(new UUID(req.getTenantIdMSB(), req.getTenantIdLSB()));
        var unitId = AgentAppUnitId.fromMsbAndLsb(req.getAgentUnitIdMSB(), req.getAgentUnitIdLSB());
        long lastSeenTs = logChunkBuffer.latestTailLineTs(tenantId, unitId);
        long allowedLastSeenTs = lastSeenTs == 0 ? 0 : Math.max(System.currentTimeMillis() - MAX_REPLAY_LOOKBACK_MS, lastSeenTs);
        ServerToAgent msg = ServerToAgent.newBuilder()
                .setStartLogStream(StartLogStream.newBuilder()
                        .setUnitId(req.getUnitIdentifier())
                        .setProjectName(req.getProjectName())
                        .setLastSeenTs(allowedLastSeenTs))
                .build();
        pushLogStreamMsg(agentId, msg, "Start");
    }

    @Override
    public void stopLogStream(AgentId agentId, String projectName, String unitIdentifier) {
        ServerToAgent msg = ServerToAgent.newBuilder()
                .setStopLogStream(StopLogStream.newBuilder()
                        .setUnitId(unitIdentifier)
                        .setProjectName(projectName))
                .build();
        pushLogStreamMsg(agentId, msg, "Stop");
    }

    private void pushLogStreamMsg(AgentId agentId, ServerToAgent msg, String kind) {
        try {
            push(agentId, msg);
        } catch (AgentSessionNotFoundException e) {
            log.trace("[{}] No local session for agent, dropping {} log stream request", agentId, kind);
        }
    }

    private void processInboundMessage(AgentSession session, AgentToServer msg) {
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder()
                .session(session)
                .msg(msg)
                .build();
        inboundExecutor.submit(session.getState().getAgentId(), () -> {
            try {
                inboundMessageDispatcher.process(ctx);
            } catch (Exception e) {
                log.error("[{}] Failed to process inbound message", session.getState().getAgentId(), e);
            }
        });
    }

    private void tryInitSession(AgentToServer msg, ServerCallStreamObserver<ServerToAgent> responseObserver,
                                AtomicReference<AgentSession> sessionRef, AtomicBoolean initializing) {
        if (msg.hasProvision()) {
            handleProvisionRequest(msg.getProvision(), responseObserver);
            return;
        }
        if (!msg.hasHello()) {
            initializing.set(false);
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("Hello Message must come before any other message")
                    .asRuntimeException());
            return;
        }
        ensureStateInit(msg, responseObserver, sessionRef, initializing);
    }

    private void handleProvisionRequest(ProvisionRequest request, ServerCallStreamObserver<ServerToAgent> responseObserver) {
        ProvisionResponse.Builder responseBuilder = ProvisionResponse.newBuilder();
        try {
            AgentProvisionService.ProvisionResult result = agentProvisionService.provision(
                    request.getProvisionKey(), request.getProvisionSecret());
            if (result.success()) {
                responseBuilder.setSuccess(true)
                        .setRoutingKey(result.routingKey())
                        .setRoutingSecret(result.routingSecret());
            } else {
                responseBuilder.setSuccess(false).setErrorMessage(result.errorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to provision agent", e);
            responseBuilder.setSuccess(false).setErrorMessage(PROVISION_FAILURE_MESSAGE);
        }
        responseObserver.onNext(ServerToAgent.newBuilder().setProvisionResponse(responseBuilder.build()).build());
        responseObserver.onCompleted();
    }

    private void ensureStateInit(AgentToServer msg, ServerCallStreamObserver<ServerToAgent> responseObserver,
                                 AtomicReference<AgentSession> sessionRef, AtomicBoolean initializing) {
        BaseAgentSession session = new BaseAgentSession(writer, responseObserver, maxPendingMessagesPerSession);
        Optional<Status> optErr;
        try {
            optErr = agentSessionService.onConnected(session, msg.getHello());
        } catch (Exception e) {
            handleInitFailure(session, initializing, responseObserver, e);
            return;
        }
        if (optErr.isPresent()) {
            log.warn("The state couldn't be initialized: {}", optErr.get());
            initializing.set(false);
            agentSessionService.onError(session);
            responseObserver.onError(optErr.get().asRuntimeException());
            return;
        }
        sessionRef.set(session);
        session.push(AgentMsgConstructorUtils.helloSuccessResponse());
        resumeEvents(session);
    }

    private void resumeEvents(AgentSession session) {
        var agent = session.getState().getAgent();
        scheduleResume(agent.getTenantId(), agent.getId(), session);
    }

    private void scheduleResume(TenantId tenantId, AgentId agentId, AgentSession session) {
        long maxDelayMs = agentCtx.getReconnectResumeMaxDelayMs();
        long delayMs = maxDelayMs > 0 ? ThreadLocalRandom.current().nextLong(maxDelayMs + 1) : 0;
        agentCtx.getReconnectResumeScheduler().schedule(() -> submitResume(tenantId, agentId, session), delayMs, TimeUnit.MILLISECONDS);
    }

    private void submitResume(TenantId tenantId, AgentId agentId, AgentSession session) {
        if (sessions.getByAgentId(agentId) != session) {
            log.trace("[{}][{}] Skipping resume-on-reconnect, session is no longer active", tenantId, agentId);
            return;
        }
        try {
            agentCtx.getAgentEventExecutor().execute(() -> agentEventProcessor.resumeEventsOnReconnect(tenantId, agentId));
        } catch (RejectedExecutionException e) {
            log.warn("[{}][{}] Agent event executor saturated, retrying resume-on-reconnect after delay", tenantId, agentId);
            scheduleResume(tenantId, agentId, session);
        }
    }

    private void handleInitFailure(AgentSession session, AtomicBoolean initializing,
                                   StreamObserver<ServerToAgent> responseObserver, Throwable t) {
        log.error("Failed to initialize agent session", t);
        initializing.set(false);
        agentSessionService.onError(session);
        responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to initialize session")
                .withCause(t)
                .asRuntimeException());
    }
}
