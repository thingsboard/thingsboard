// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;
import org.thingsboard.server.service.edge.rpc.session.manager.KafkaBasedEdgeGrpcSessionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultZombieSessionCleanupService implements ZombieSessionCleanupService {

    @Autowired
    private EdgeSessionsHolder edgeSessionsHolder;

    private final Queue<EdgeGrpcSessionManager> zombieSessions;
    private final ScheduledExecutorService zombieSessionsExecutorService;

    public DefaultZombieSessionCleanupService() {
        this.zombieSessions = new ConcurrentLinkedQueue<>();
        this.zombieSessionsExecutorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("zombie-sessions");
    }

    @PostConstruct
    public void init() {
        this.zombieSessionsExecutorService.scheduleAtFixedRate(this::cleanupZombieSessions, 30, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (zombieSessionsExecutorService != null && !zombieSessionsExecutorService.isShutdown()) {
            zombieSessionsExecutorService.shutdown();
        }
    }

    @Override
    public void add(EdgeGrpcSessionManager session) {
        zombieSessions.add(session);
    }

    private void cleanupZombieSessions() {
        try {
            // remove by identity: the edge may have reconnected since the zombie scan, and that newer
            // session must not be evicted by the cleanup of the one it replaced
            tryToDestroyZombieSessions(getZombieSessions(edgeSessionsHolder.getSessions().values()),
                    s -> edgeSessionsHolder.removeByEdgeIdIfCurrent(s.getState().getEdgeId(), s.getState().getSessionId()));

            tryToDestroyZombieSessions(getZombieSessions(edgeSessionsHolder.getSessionsById().values()),
                    s -> edgeSessionsHolder.removeBySessionId(s.getState().getSessionId()));

            zombieSessions.removeIf(zombie -> {
                EdgeSessionState state = zombie.getState();
                if (zombie.destroy()) {
                    log.info("[{}][{}] Successfully cleaned up zombie session [{}] for edge [{}].",
                            state.getTenantId(), state.getEdgeId(), state.getSessionId(), state.getEdge().getName());
                    return true;
                } else {
                    log.warn("[{}][{}] Failed to remove zombie session [{}] for edge [{}].",
                            state.getTenantId(), state.getEdgeId(), state.getSessionId(), state.getEdge().getName());
                    return false;
                }
            });
        } catch (Exception e) {
            log.warn("Failed to cleanup zombie sessions", e);
        }
    }

    private List<EdgeGrpcSessionManager> getZombieSessions(Collection<EdgeGrpcSessionManager> sessions) {
        List<EdgeGrpcSessionManager> result = new ArrayList<>();
        for (EdgeGrpcSessionManager session : sessions) {
            if (isKafkaSessionAndZombie(session)) {
                result.add(session);
            }
        }
        return result;
    }

    private boolean isKafkaSessionAndZombie(EdgeGrpcSessionManager session) {
        if (session instanceof KafkaBasedEdgeGrpcSessionManager kafkaSession) {
            EdgeSessionState sessionState = kafkaSession.getState();
            log.debug("[{}] kafkaSession.isConnected() = {}, kafkaSession.getConsumer().getConsumer().isStopped() = {}",
                    sessionState.getEdgeId(),
                    sessionState.isConnected(),
                    kafkaSession.getConsumer() != null ? kafkaSession.getConsumer().getConsumer() != null ? kafkaSession.getConsumer().getConsumer().isStopped() : null : null);
            return !sessionState.isConnected() &&
                    kafkaSession.getConsumer() != null &&
                    kafkaSession.getConsumer().getConsumer() != null &&
                    !kafkaSession.getConsumer().getConsumer().isStopped();
        }
        return false;
    }

    private void tryToDestroyZombieSessions(List<EdgeGrpcSessionManager> sessionsToRemove, Function<EdgeGrpcSessionManager, EdgeGrpcSessionManager> removeFunc) {
        for (EdgeGrpcSessionManager toRemove : sessionsToRemove) {
            log.info("[{}] Destroying session for edge because edge is not connected", toRemove.getState().getEdge().getId());
            if (toRemove.destroy()) {
                removeFunc.apply(toRemove);
            }
        }
    }
}
