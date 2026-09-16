/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.SimpleTbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.executors.GrpcCallbackExecutorService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the reconnect race between a session that is being torn down and the session that has already
 * taken the edge over: the teardown must not detach, cancel the edge event check of, or consume the new
 * events flag of the session that replaced it.
 */
class EdgeGrpcServiceReconnectTest {

    // long enough that an armed check never actually fires during a test
    private static final long NO_RECORDS_SLEEP_INTERVAL_MS = 60_000L;
    private static final String THIS_NODE = "test-core";

    private EdgeGrpcService service;
    private ScheduledExecutorService edgeEventProcessingExecutorService;
    private TbTransactionalCache<EdgeId, String> edgeIdServiceIdCache;

    private TenantId tenantId;
    private EdgeId edgeId;
    private Edge edge;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        edgeId = new EdgeId(UUID.randomUUID());
        edge = new Edge(edgeId);
        edge.setTenantId(tenantId);
        edge.setName("test-edge");

        EdgeEventStorageSettings storageSettings = new EdgeEventStorageSettings();
        ReflectionTestUtils.setField(storageSettings, "noRecordsSleepInterval", NO_RECORDS_SLEEP_INTERVAL_MS);

        GrpcCallbackExecutorService grpcCallbackExecutorService = mock(GrpcCallbackExecutorService.class);
        // run future callbacks inline so the re-arm is observable without waiting on a pool
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(grpcCallbackExecutorService).execute(any());

        EdgeContextComponent ctx = mock(EdgeContextComponent.class);
        when(ctx.getEdgeEventStorageSettings()).thenReturn(storageSettings);
        when(ctx.getGrpcCallbackExecutorService()).thenReturn(grpcCallbackExecutorService);
        when(ctx.getRuleProcessor()).thenReturn(mock(NotificationRuleProcessor.class));

        TbServiceInfoProvider serviceInfoProvider = mock(TbServiceInfoProvider.class);
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);

        edgeIdServiceIdCache = mock(TbTransactionalCache.class);
        // this node is the recorded owner of the edge, which is what a live local session implies
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));
        edgeEventProcessingExecutorService = Executors.newSingleThreadScheduledExecutor();

        service = new EdgeGrpcService();
        ReflectionTestUtils.setField(service, "ctx", ctx);
        ReflectionTestUtils.setField(service, "tsSubService", mock(TelemetrySubscriptionService.class));
        ReflectionTestUtils.setField(service, "clusterService", mock(TbClusterService.class));
        ReflectionTestUtils.setField(service, "serviceInfoProvider", serviceInfoProvider);
        ReflectionTestUtils.setField(service, "edgeIdServiceIdCache", edgeIdServiceIdCache);
        ReflectionTestUtils.setField(service, "edgeEventProcessingExecutorService", edgeEventProcessingExecutorService);
    }

    @AfterEach
    void tearDown() {
        edgeEventProcessingExecutorService.shutdownNow();
    }

    @Test
    void checkIsArmedForTheCurrentSession() {
        EdgeGrpcSession session = newSession();
        sessions().put(edgeId, session);

        scheduleEdgeEventsCheck(session);

        ScheduledFuture<?> armed = edgeEventChecks().get(edgeId);
        assertThat((Object) armed).as("check armed for the session that owns the edge").isNotNull();
        assertThat(armed.isCancelled()).isFalse();
    }

    @Test
    void checkIsNotArmedForASupersededSession() {
        EdgeGrpcSession current = newSession();
        EdgeGrpcSession superseded = newSession();
        sessions().put(edgeId, current);
        scheduleEdgeEventsCheck(current);
        ScheduledFuture<?> armedByCurrent = edgeEventChecks().get(edgeId);

        scheduleEdgeEventsCheck(superseded);

        assertThat((Object) edgeEventChecks().get(edgeId))
                .as("the superseded session must neither cancel nor replace the current session's check")
                .isSameAs(armedByCurrent);
        assertThat(armedByCurrent.isCancelled()).isFalse();
    }

    @Test
    void disconnectOfTheCurrentSessionDetachesItAndCancelsItsCheck() {
        EdgeGrpcSession session = newSession();
        onEdgeConnect(session);
        ScheduledFuture<?> armedCheck = edgeEventChecks().get(edgeId);

        onEdgeDisconnect(session);

        assertThat(sessions()).doesNotContainKey(edgeId);
        assertThat(sessionsById()).doesNotContainKey(session.getSessionId());
        assertThat(armedCheck.isCancelled()).as("the detached session's check is cancelled").isTrue();
        verify(edgeIdServiceIdCache).evict(edgeId);
        verify(session).destroy();
    }

    @Test
    void disconnectOfASupersededSessionKeepsTheReconnectedSessionAndItsCheck() {
        EdgeGrpcSession first = newSession();
        onEdgeConnect(first);
        // the edge reconnects before the gRPC stream of the first session is closed
        EdgeGrpcSession second = newSession();
        onEdgeConnect(second);
        ScheduledFuture<?> armedBySecond = edgeEventChecks().get(edgeId);

        onEdgeDisconnect(first);

        assertThat(sessions().get(edgeId)).as("the reconnected session stays registered").isSameAs(second);
        assertThat(sessionsById()).containsKey(second.getSessionId());
        assertThat((Object) edgeEventChecks().get(edgeId))
                .as("the check armed by the reconnected session survives the teardown of the one it replaced")
                .isSameAs(armedBySecond);
        assertThat(armedBySecond.isCancelled()).isFalse();
        verify(edgeIdServiceIdCache, never()).evict(edgeId);
    }

    @Test
    void disconnectDoesNotClearTheNewEventsFlag() {
        EdgeGrpcSession session = newSession();
        onEdgeConnect(session);
        assertThat(newEvents()).containsEntry(edgeId, true);

        onEdgeDisconnect(session);

        assertThat(newEvents())
                .as("clearing the flag here would wipe the one a reconnecting session has just raised")
                .containsEntry(edgeId, true);
    }

    @Test
    void checkOfTheCurrentSessionConsumesTheFlagAndReArms() throws Exception {
        EdgeGrpcSession session = newSession();
        when(session.processEdgeEvents()).thenReturn(Futures.immediateFuture(false));
        sessions().put(edgeId, session);
        newEvents().put(edgeId, true);
        migrationProcessed().put(edgeId, Boolean.TRUE);

        processEvents(session);

        assertThat(newEvents()).containsEntry(edgeId, false);
        verify(session).processHighPriorityEvents();
        verify(session).processEdgeEvents();
        assertThat((Object) edgeEventChecks().get(edgeId)).as("the check re-arms itself").isNotNull();
    }

    @Test
    void checkOfASupersededSessionLeavesTheFlagForTheSessionThatOwnsTheEdge() throws Exception {
        EdgeGrpcSession superseded = newSession();
        EdgeGrpcSession current = newSession();
        sessions().put(edgeId, current);
        newEvents().put(edgeId, true);
        migrationProcessed().put(edgeId, Boolean.TRUE);

        // cancel(true) cannot stop a check that has already started, so it runs against the old session
        processEvents(superseded);

        assertThat(newEvents())
                .as("consuming the flag here would leave the reconnected session with nothing to fetch")
                .containsEntry(edgeId, true);
        verify(superseded, never()).processHighPriorityEvents();
        verify(superseded, never()).processEdgeEvents();
        assertThat(edgeEventChecks()).doesNotContainKey(edgeId);
    }

    @Test
    void zombieCleanupCancelsTheCheckOfTheSessionItRemoves() {
        KafkaEdgeGrpcSession zombie = newZombieSession();
        when(zombie.destroy()).thenReturn(true);
        sessions().put(edgeId, zombie);
        scheduleEdgeEventsCheck(zombie);
        ScheduledFuture<?> armedByZombie = edgeEventChecks().get(edgeId);

        cleanupZombieSessions();

        assertThat(sessions()).doesNotContainKey(edgeId);
        assertThat(armedByZombie.isCancelled())
                .as("the removed zombie must not leave an armed check behind")
                .isTrue();
    }

    @Test
    void zombieCleanupKeepsASessionThatReconnectedDuringTheSweep() {
        KafkaEdgeGrpcSession zombie = newZombieSession();
        EdgeGrpcSession reconnected = newSession();
        sessions().put(edgeId, zombie);
        // the edge reconnects while the zombie is being destroyed, i.e. after the scan picked it up
        when(zombie.destroy()).thenAnswer(invocation -> {
            sessions().put(edgeId, reconnected);
            scheduleEdgeEventsCheck(reconnected);
            return true;
        });

        cleanupZombieSessions();

        assertThat(sessions().get(edgeId)).isSameAs(reconnected);
        ScheduledFuture<?> armedByReconnected = edgeEventChecks().get(edgeId);
        assertThat((Object) armedByReconnected).as("the reconnected session keeps its check").isNotNull();
        assertThat(armedByReconnected.isCancelled()).isFalse();
    }

    private EdgeGrpcSession newSession() {
        EdgeGrpcSession session = mock(EdgeGrpcSession.class);
        stubSession(session);
        return session;
    }

    private KafkaEdgeGrpcSession newZombieSession() {
        KafkaEdgeGrpcSession session = mock(KafkaEdgeGrpcSession.class);
        stubSession(session);
        @SuppressWarnings("unchecked")
        QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumerManager = mock(QueueConsumerManager.class);
        @SuppressWarnings("unchecked")
        TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer = mock(TbQueueConsumer.class);
        when(consumerManager.getConsumer()).thenReturn(consumer);
        when(consumer.isStopped()).thenReturn(false);
        when(session.getConsumer()).thenReturn(consumerManager);
        return session;
    }

    private void stubSession(EdgeGrpcSession session) {
        when(session.getEdge()).thenReturn(edge);
        when(session.getTenantId()).thenReturn(tenantId);
        when(session.getSessionId()).thenReturn(UUID.randomUUID());
        when(session.isConnected()).thenReturn(false);
        when(session.destroy()).thenReturn(true);
    }

    private void onEdgeConnect(EdgeGrpcSession session) {
        ReflectionTestUtils.invokeMethod(service, "onEdgeConnect", edgeId, session);
    }

    private void onEdgeDisconnect(EdgeGrpcSession session) {
        ReflectionTestUtils.invokeMethod(service, "onEdgeDisconnect", edge, session.getSessionId());
    }

    private void scheduleEdgeEventsCheck(EdgeGrpcSession session) {
        ReflectionTestUtils.invokeMethod(service, "scheduleEdgeEventsCheck", session);
    }

    private void processEvents(EdgeGrpcSession session) {
        ReflectionTestUtils.invokeMethod(service, "processEvents", session, edgeId, tenantId);
    }

    private void cleanupZombieSessions() {
        ReflectionTestUtils.invokeMethod(service, "cleanupZombieSessions");
    }

    private ConcurrentMap<EdgeId, EdgeGrpcSession> sessions() {
        return field("sessions");
    }

    private ConcurrentMap<UUID, EdgeGrpcSession> sessionsById() {
        return field("sessionsById");
    }

    private ConcurrentMap<EdgeId, ScheduledFuture<?>> edgeEventChecks() {
        return field("sessionEdgeEventChecks");
    }

    private Map<EdgeId, Boolean> newEvents() {
        return field("sessionNewEvents");
    }

    private ConcurrentMap<EdgeId, Boolean> migrationProcessed() {
        return field("edgeEventsMigrationProcessed");
    }

    @SuppressWarnings("unchecked")
    private <T> T field(String name) {
        return (T) ReflectionTestUtils.getField(service, name);
    }

}
