// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A teardown that races with a reconnect must not act on behalf of the session that has taken the
 * edge over: neither by removing it from the holder nor by treating it as current.
 */
class EdgeSessionsHolderTest {

    private EdgeSessionsHolder holder;
    private Edge edge;
    private EdgeId edgeId;

    @BeforeEach
    void setUp() {
        holder = new EdgeSessionsHolder();
        edgeId = new EdgeId(UUID.randomUUID());
        edge = new Edge(edgeId);
        edge.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        edge.setName("test-edge");
    }

    @Test
    void removeByEdgeIdIfCurrentRemovesTheRegisteredSession() {
        EdgeGrpcSessionManager session = newSession();
        holder.put(session);

        EdgeGrpcSessionManager removed = holder.removeByEdgeIdIfCurrent(edgeId, session.getState().getSessionId());

        assertThat(removed).isSameAs(session);
        assertThat(holder.getByEdgeId(edgeId)).isNull();
    }

    @Test
    void removeByEdgeIdIfCurrentKeepsTheSessionThatTookTheEdgeOver() {
        EdgeGrpcSessionManager first = newSession();
        EdgeGrpcSessionManager second = newSession();
        holder.put(first);
        holder.put(second);

        EdgeGrpcSessionManager removed = holder.removeByEdgeIdIfCurrent(edgeId, first.getState().getSessionId());

        assertThat(removed).as("the superseded session owns nothing to remove").isNull();
        assertThat(holder.getByEdgeId(edgeId))
                .as("the reconnected session stays registered")
                .isSameAs(second);
    }

    @Test
    void isCurrentOnlyForTheSessionRegisteredForTheEdge() {
        EdgeGrpcSessionManager first = newSession();
        EdgeGrpcSessionManager second = newSession();
        holder.put(first);
        assertThat(holder.isCurrent(first)).isTrue();

        holder.put(second);

        assertThat(holder.isCurrent(second)).isTrue();
        assertThat(holder.isCurrent(first))
                .as("a presence check would still say true here, which is the bug this guards")
                .isFalse();
    }

    @Test
    void isCurrentIsFalseWhenTheEdgeHasNoRegisteredSession() {
        assertThat(holder.isCurrent(newSession())).isFalse();
    }

    private EdgeGrpcSessionManager newSession() {
        EdgeSessionState state = new EdgeSessionState();
        state.setEdge(edge);
        EdgeGrpcSessionManager session = mock(EdgeGrpcSessionManager.class);
        when(session.getState()).thenReturn(state);
        return session;
    }

}
