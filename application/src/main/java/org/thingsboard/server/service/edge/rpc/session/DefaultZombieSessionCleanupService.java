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
package org.thingsboard.server.service.edge.rpc.session;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
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
public class DefaultZombieSessionCleanupService implements ZombieSessionCleanupService {

    @Autowired
    private EdgeSessionsHolder edgeSessionsHolder;

    private final Queue<EdgeGrpcSessionManager> zombieSessions;
    private final ScheduledExecutorService zombieSessionsExecutorService;

    public DefaultZombieSessionCleanupService() {
        this.zombieSessions = new ConcurrentLinkedQueue<>();
        this.zombieSessionsExecutorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("zombie-sessions");
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
            tryToDestroyZombieSessions(getZombieSessions(edgeSessionsHolder.getSessions().values()),
                    s -> edgeSessionsHolder.removeByEdgeId(s.getState().getEdge().getId()));

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
            log.warn("Failed to cleanup kafka sessions", e);
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
