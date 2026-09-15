// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Data
@Slf4j
@Component
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class EdgeSessionsHolder {

    private final ConcurrentMap<EdgeId, EdgeGrpcSessionManager> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, EdgeGrpcSessionManager> sessionsById = new ConcurrentHashMap<>();

    public void forEach(Consumer<EdgeGrpcSessionManager> consumer) {
        Set<EdgeGrpcSessionManager> unique = new HashSet<>(sessions.values());
        unique.addAll(sessionsById.values());

        unique.forEach(consumer);
    }

    public void put(EdgeGrpcSessionManager session) {
        UUID sessionId = session.getState().getSessionId();
        EdgeId edgeId = session.getState().getEdgeId();
        sessionsById.put(sessionId, session);
        sessions.put(edgeId, session);
    }

    public EdgeGrpcSessionManager getByEdgeId(EdgeId id) {
        return sessions.get(id);
    }

    public boolean hasByEdgeId(EdgeId id) {
        return sessions.containsKey(id);
    }

    public EdgeGrpcSessionManager removeByEdgeId(EdgeId id) {
        return sessions.remove(id);
    }

    public EdgeGrpcSessionManager removeBySessionId(UUID sessionId) {
        return sessionsById.remove(sessionId);
    }

    public void remove(EdgeGrpcSessionManager session) {
        if (session == null) {
            log.warn("Can't remove session from holder because it's null");
            return;
        }
        EdgeSessionState sessionState = session.getState();
        removeByEdgeId(sessionState.getEdgeId());
        removeBySessionId(sessionState.getSessionId());
    }
}
