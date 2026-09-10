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
import java.util.concurrent.atomic.AtomicReference;
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

    /**
     * Whether the given session is still the one registered for its edge. A session that has been
     * superseded must not arm or run work on the edge's behalf - the session that replaced it owns
     * that now - and a plain "some session exists for this edge" check does not catch that.
     */
    public boolean isCurrent(EdgeGrpcSessionManager session) {
        EdgeSessionState state = session.getState();
        return state != null && sessions.get(state.getEdgeId()) == session;
    }

    /**
     * Removes the edge's registered session and returns it, but only if it is still the given session.
     * Returns null when another session has taken the edge over in the meantime, so that a teardown
     * racing with a reconnect cannot act on behalf of a session it no longer owns. The check and the
     * removal are one atomic step.
     * <p>
     * Only cheap non-blocking work may run inside the remapping function: it holds the bin lock of
     * {@link #sessions} for this edge, so a slow teardown such as
     * {@link EdgeGrpcSessionManager#destroyAndMarkAsZombieIfFailed()} has to be done by the caller
     * once compute has returned.
     */
    public EdgeGrpcSessionManager removeByEdgeIdIfCurrent(EdgeId id, UUID sessionId) {
        AtomicReference<EdgeGrpcSessionManager> removed = new AtomicReference<>();
        sessions.compute(id, (edgeId, current) -> {
            if (current == null || !sessionId.equals(current.getState().getSessionId())) {
                return current;
            }
            removed.set(current);
            return null;
        });
        return removed.get();
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
