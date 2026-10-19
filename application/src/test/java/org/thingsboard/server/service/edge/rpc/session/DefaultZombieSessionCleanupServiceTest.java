// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;
import org.thingsboard.server.service.edge.rpc.session.manager.KafkaBasedEdgeGrpcSessionManager;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The zombie sweep destroys a session and then removes it from the holder. If the edge reconnects in
 * between, the removal must not take the live session with it.
 */
class DefaultZombieSessionCleanupServiceTest {

    private DefaultZombieSessionCleanupService service;
    private EdgeSessionsHolder holder;
    private Edge edge;
    private EdgeId edgeId;

    @BeforeEach
    void setUp() {
        edgeId = new EdgeId(UUID.randomUUID());
        edge = new Edge(edgeId);
        edge.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        edge.setName("test-edge");

        holder = new EdgeSessionsHolder();
        service = new DefaultZombieSessionCleanupService();
        ReflectionTestUtils.setField(service, "edgeSessionsHolder", holder);
    }

    @AfterEach
    void tearDown() {
        service.destroy();
    }

    @Test
    void cleanupRemovesTheZombieWhenItStillOwnsTheEdge() {
        KafkaBasedEdgeGrpcSessionManager zombie = newZombieSession();
        when(zombie.destroy()).thenReturn(true);
        holder.put(zombie);

        cleanupZombieSessions();

        assertThat(holder.getByEdgeId(edgeId)).isNull();
    }

    @Test
    void cleanupKeepsASessionThatReconnectedDuringTheSweep() {
        KafkaBasedEdgeGrpcSessionManager zombie = newZombieSession();
        EdgeGrpcSessionManager reconnected = newSession();
        holder.put(zombie);
        // the edge reconnects while the zombie is being destroyed, i.e. after the scan picked it up.
        // The sweep also passes over sessionsById, so only the first destroy stands in for the reconnect
        AtomicBoolean reconnectPending = new AtomicBoolean(true);
        when(zombie.destroy()).thenAnswer(invocation -> {
            if (reconnectPending.compareAndSet(true, false)) {
                holder.put(reconnected);
            }
            return true;
        });

        cleanupZombieSessions();

        assertThat(holder.getByEdgeId(edgeId))
                .as("removing by edge id alone would evict the live session here")
                .isSameAs(reconnected);
    }

    private void cleanupZombieSessions() {
        ReflectionTestUtils.invokeMethod(service, "cleanupZombieSessions");
    }

    private EdgeGrpcSessionManager newSession() {
        EdgeGrpcSessionManager session = mock(EdgeGrpcSessionManager.class);
        when(session.getState()).thenReturn(newState());
        return session;
    }

    private KafkaBasedEdgeGrpcSessionManager newZombieSession() {
        KafkaBasedEdgeGrpcSessionManager session = mock(KafkaBasedEdgeGrpcSessionManager.class);
        when(session.getState()).thenReturn(newState());
        @SuppressWarnings("unchecked")
        QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumerManager = mock(QueueConsumerManager.class);
        @SuppressWarnings("unchecked")
        TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer = mock(TbQueueConsumer.class);
        when(consumerManager.getConsumer()).thenReturn(consumer);
        when(consumer.isStopped()).thenReturn(false);
        when(session.getConsumer()).thenReturn(consumerManager);
        return session;
    }

    private EdgeSessionState newState() {
        EdgeSessionState state = new EdgeSessionState();
        state.setEdge(edge);
        state.setConnected(false);
        return state;
    }

}
