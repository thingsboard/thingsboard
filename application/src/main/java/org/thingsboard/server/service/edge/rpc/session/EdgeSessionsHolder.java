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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Data
@Slf4j
@Component
public class EdgeSessionsHolder {

    private final ConcurrentMap<EdgeId, EdgeGrpcSessionManager> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, EdgeGrpcSessionManager> sessionsById = new ConcurrentHashMap<>();

    public void forEach(Consumer<EdgeGrpcSessionManager> consumer) {
        sessions.values().forEach(consumer);
        sessionsById.values().forEach(consumer);
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
        }
        EdgeSessionState sessionState = session.getState();
        removeByEdgeId(sessionState.getEdge().getId()); // todo: react to warnings
        removeBySessionId(sessionState.getSessionId());
    }
}
