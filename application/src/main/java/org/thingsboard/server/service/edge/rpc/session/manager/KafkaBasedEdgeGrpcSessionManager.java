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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbKafkaComponent;
import org.thingsboard.server.service.edge.rpc.DownlinkMessageMapper;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.processor.PostgresGeneralEdgeEventsDispatcher;
import org.thingsboard.server.service.edge.rpc.session.EdgeSessionsHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@TbKafkaComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class KafkaBasedEdgeGrpcSessionManager extends AbstractEdgeGrpcSessionManager {

    private static final int NO_INITIAL_DELAY_VALUE = 0;

    private final TbCoreQueueFactory tbCoreQueueFactory;
    private final DownlinkMessageMapper downlinkMessageMapper;
    private final TopicService topicService;
    private final KafkaAdmin kafkaAdmin;
    private final EdgeSessionsHolder sessions;

    private final AtomicReference<ScheduledFuture<?>> initMigrationAndProcessingFutureRef = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> highPriorityProcessingFutureRef = new AtomicReference<>();
    private final Lock initLock = new ReentrantLock();
    private volatile boolean migrationProcessed;
    private volatile boolean isHighPriorityProcessing;
    @Getter
    private QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer;
    private PostgresGeneralEdgeEventsDispatcher psqlGeneralEdgeEventsDispatcher;
    private ExecutorService consumerExecutor;

    @Override
    public void onEdgeConnect() {
        scheduleMigrationAndProcessing();
    }

    @Override
    public void onEdgeEventUpdate() {}

    @Override
    public void onEdgeDisconnect() {}

    @Override
    public void onEdgeRemoval() {
        EdgeSessionState state = getState();
        String topic = topicService
                .buildEdgeEventNotificationsTopicPartitionInfo(state.getTenantId(), state.getEdgeId())
                .getTopic();
        kafkaAdmin.deleteTopic(topic);
        kafkaAdmin.deleteConsumerGroup(topic);
    }

    @Override
    public boolean destroy() {
        cancelHighPriorityProcessing();
        EdgeSessionState state = getState();
        try {
            if (consumer != null) {
                log.info("[{}][{}] Stopping edge event consumer...", state.getTenantId(), state.getEdgeId());
                consumer.stop();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to stop edge event consumer", state.getTenantId(), state.getEdgeId(), e);
            return false;
        }
        consumer = null;
        try {
            if (consumerExecutor != null && !consumerExecutor.isShutdown()) {
                consumerExecutor.shutdown();
                awaitConsumerTermination();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to shutdown edge event consumer executor", state.getTenantId(), state.getEdgeId(), e);
            return false;
        }
        return true;
    }

    private void scheduleMigrationAndProcessing() {
        cancelMigrationAndProcessingInit();
        EdgeSessionState state = getSession().getState();
        TenantId tenantId = state.getTenantId();
        EdgeId edgeId = state.getEdgeId();

        if (!sessions.hasByEdgeId(edgeId)) {
            log.debug("[{}] Session was removed and edge event consumer must not be started [{}]",
                    tenantId, edgeId.getId());
            return;
        }
        ScheduledFuture<?> initMigrationAndProcessingTask = ctx.getEdgeEventProcessingExecutorService().schedule(() -> {
            log.info("[{}] Edge event processing task started for edge [{}]", tenantId, edgeId);
            initLock.lock();
            try {
                processRemainingPostgresEventsIfRequired(edgeId);
                if (migrationProcessed) {
                    initAndLaunchConsumerOrRetry(tenantId, edgeId, state);
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, edgeId, e);
            }
            finally {
                initLock.unlock();
            }
        }, ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval(), TimeUnit.MILLISECONDS);

        initMigrationAndProcessingFutureRef.set(initMigrationAndProcessingTask);
    }

    private void initAndLaunchConsumerOrRetry(TenantId tenantId, EdgeId edgeId, EdgeSessionState state) {
        if (initAndLaunchConsumer(tenantId, edgeId, state)) {
            launchProcessingOfHighPriorityEvents(tenantId, edgeId);
        } else {
            scheduleMigrationAndProcessing();
        }
    }

    private void launchProcessingOfHighPriorityEvents(TenantId tenantId, EdgeId edgeId) {
        cancelHighPriorityProcessing();
        ScheduledFuture<?> highPriorityProcessingTask = ctx.getEdgeEventProcessingExecutorService().scheduleAtFixedRate(() -> {
            try {
                isHighPriorityProcessing = true;
                session.processHighPriorityEvents();
                isHighPriorityProcessing = false;
            } catch (Exception e) {
                log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, edgeId, e);
            }
        }, NO_INITIAL_DELAY_VALUE, ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval(), TimeUnit.MILLISECONDS);
        highPriorityProcessingFutureRef.set(highPriorityProcessingTask);
    }

    private boolean initAndLaunchConsumer(TenantId tenantId, EdgeId edgeId, EdgeSessionState state) {
        if (!state.isConnected() || state.isSyncInProgress() || isHighPriorityProcessing) {
            log.warn("[{}][{}] Session is not ready (connected={}, syncInProgress={}, highPriority={}), skip starting edge event consumer",
                    tenantId, edgeId, state.isConnected(), state.isSyncInProgress(), isHighPriorityProcessing);
            return false;
        }
        if (isConsumerInitializationRequired()) {
            initConsumerAndExecutor(tenantId, edgeId, state);
        }
        return true;
    }

    private void initConsumerAndExecutor(TenantId tenantId, EdgeId edgeId, EdgeSessionState state) {
        try {
            if (consumerExecutor != null && !consumerExecutor.isShutdown()) {
                try {
                    consumerExecutor.shutdown();
                    awaitConsumerTermination();
                } catch (Exception e) {
                    log.warn("[{}][{}] Failed to shutdown previous consumer executor", tenantId, edgeId, e);
                }
            }
            this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-event-consumer"));
            this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>builder()
                    .name("TB Edge events [" + edgeId + "]")
                    .msgPackProcessor(this::processMsgs)
                    .pollInterval(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval())
                    .consumerCreator(() -> tbCoreQueueFactory.createEdgeEventMsgConsumer(tenantId, edgeId))
                    .consumerExecutor(consumerExecutor)
                    .threadPrefix("edge-events-" + edgeId)
                    .build();
            consumer.subscribe();
            consumer.launch();
        } catch (Exception e) {
            destroy();
            log.warn("[{}][{}] Failed to start edge event consumer", state.getSessionId(), edgeId, e);
        }
    }

    private boolean isConsumerInitializationRequired() {
        return consumer == null || (consumer.getConsumer() != null && consumer.getConsumer().isStopped());
    }

    private void processMsgs(List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer) {
        EdgeSessionState state = getSession().getState();
        TenantId tenantId = state.getTenantId();
        EdgeId edgeId = state.getEdgeId();

        log.trace("[{}][{}] starting processing edge events", tenantId, edgeId);
        if (!state.isConnected() || state.isSyncInProgress() || isHighPriorityProcessing) {
            log.debug("[{}][{}] edge not connected, edge sync is not completed or high priority processing in progress, " +
                            "connected = {}, sync in progress = {}, high priority in progress = {}. Skipping iteration",
                    tenantId, edgeId, state.isConnected(), state.isSyncInProgress(), isHighPriorityProcessing);
            return;
        }
        List<EdgeEvent> edgeEvents = new ArrayList<>();
        for (TbProtoQueueMsg<ToEdgeEventNotificationMsg> msg : msgs) {
            EdgeEvent edgeEvent = ProtoUtils.fromProto(msg.getValue().getEdgeEventMsg());
            edgeEvents.add(edgeEvent);
        }
        List<DownlinkMsg> downlinkMsgsPack = downlinkMessageMapper.convertToDownlinkMsgsPack(state, edgeEvents);
        try {
            boolean isInterrupted = session.sendDownlinkMsgsPack(downlinkMsgsPack).get();
            if (isInterrupted) {
                log.debug("[{}][{}] Send downlink messages task was interrupted", tenantId, edgeId);
            } else {
                consumer.commit();
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process downlink messages", tenantId, edgeId, e);
        }
    }

    private void cancelMigrationAndProcessingInit() {
        log.trace("[{}] cancelling edge migration & processing init for edge", getState().getEdgeId());
        cancelFuture(initMigrationAndProcessingFutureRef);
    }

    private void cancelHighPriorityProcessing() {
        log.trace("[{}] cancelling high priority processing task for edge", getState().getEdgeId());
        cancelFuture(highPriorityProcessingFutureRef);
    }

    private void cancelFuture(AtomicReference<? extends Future<?>> futureReference) {
        Future<?> f = futureReference.getAndSet(null);
        if (f != null && !f.isCancelled() && !f.isDone()) {
            f.cancel(true);
        }
    }

    private void processRemainingPostgresEventsIfRequired(EdgeId edgeId) throws Exception {
        log.debug("Processing remaining postgres edge events for edge [{}]", edgeId);

        if (psqlGeneralEdgeEventsDispatcher == null) {
            psqlGeneralEdgeEventsDispatcher = new PostgresGeneralEdgeEventsDispatcher(session, ctx);
        }
        if (migrationProcessed) {
            return;
        }
        Boolean hasMorePostgresEvents = psqlGeneralEdgeEventsDispatcher.processNewEvents().get();
        if (Boolean.TRUE.equals(hasMorePostgresEvents)) {
            log.info("Migration still in progress for edge [{}]", edgeId.getId());
            scheduleMigrationAndProcessing();
        } else if (Boolean.FALSE.equals(hasMorePostgresEvents)) {
            log.info("Migration completed for edge [{}]", edgeId.getId());
            migrationProcessed = true;
        }
    }

    private void awaitConsumerTermination() {
        EdgeSessionState state = getState();
        try {
            if (!consumerExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                consumerExecutor.shutdownNow(); // todo: verify it is acceptable
            }
        } catch (InterruptedException ie) {
            log.warn("[{}][{}] Interrupted while awaiting consumer executor termination", state.getTenantId(), state.getEdgeId());
        }
    }
}
