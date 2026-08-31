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
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

@Slf4j
public class KafkaEdgeGrpcSession extends EdgeGrpcSession {

    private static final long DELIVERY_WARN_INTERVAL_MS = 60000;

    private final TopicService topicService;
    private final TbCoreQueueFactory tbCoreQueueFactory;
    private final KafkaAdmin kafkaAdmin;

    private volatile boolean isHighPriorityProcessing;

    @Getter
    private QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer;

    private ExecutorService consumerExecutor;

    public KafkaEdgeGrpcSession(EdgeContextComponent ctx, TopicService topicService, TbCoreQueueFactory tbCoreQueueFactory,
                                KafkaAdmin kafkaAdmin, StreamObserver<ResponseMsg> outputStream,
                                BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener, BiConsumer<Edge, UUID> sessionCloseListener,
                                ScheduledExecutorService sendDownlinkExecutorService, int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        super(ctx, outputStream, sessionOpenListener, sessionCloseListener, sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
        this.topicService = topicService;
        this.tbCoreQueueFactory = tbCoreQueueFactory;
        this.kafkaAdmin = kafkaAdmin;
    }

    private void processMsgs(List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer) {
        log.trace("[{}][{}] starting processing edge events", tenantId, edge.getId());
        List<EdgeEvent> edgeEvents = new ArrayList<>();
        for (TbProtoQueueMsg<ToEdgeEventNotificationMsg> msg : msgs) {
            EdgeEvent edgeEvent = ProtoUtils.fromProto(msg.getValue().getEdgeEventMsg());
            edgeEvents.add(edgeEvent);
        }
        if (deliverEdgeEvents(edgeEvents, consumer)) {
            consumer.commit();
        }
    }

    /**
     * Delivers an already polled pack, retrying it from memory for as long as the session can still deliver it.
     * <p>
     * poll() advances the consumer position before this method is ever called and nothing rewinds it, so skipping the
     * commit does not put the pack back on the queue - it only defers redelivery until the edge reconnects and a new
     * consumer resumes from the uncommitted offset. Until then the whole pack is invisible, which is how a sync used to
     * silently swallow every cloud-side change that was in flight when it started. Retrying here is the only recovery
     * path available, and it mirrors what the edge already does for uplink packs.
     *
     * @return true when the pack was delivered and the offset can be committed
     */
    private boolean deliverEdgeEvents(List<EdgeEvent> edgeEvents, TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer) {
        int attempt = 0;
        long startTs = System.currentTimeMillis();
        long nextWarnTs = startTs + DELIVERY_WARN_INTERVAL_MS;
        while (!consumer.isStopped() && isConnected()) {
            nextWarnTs = warnIfPackIsHeldForTooLong(edgeEvents.size(), startTs, nextWarnTs);
            if (!isReadyToProcessGeneralEvents()) {
                log.debug("[{}][{}] Waiting to deliver {} edge event(s), sync in progress = {}, high priority in progress = {}",
                        tenantId, edge.getId(), edgeEvents.size(), isSyncInProgress(), isHighPriorityProcessing);
                if (!sleepBeforeNextAttempt()) {
                    return false;
                }
                continue;
            }
            attempt++;
            try {
                // converted on every attempt, so that a retry picks up the current state of the entities
                if (!sendDownlinkMsgsPack(convertToDownlinkMsgsPack(edgeEvents)).get()) {
                    return true;
                }
                log.debug("[{}][{}] Send downlink messages task was interrupted on attempt {}, going to retry", tenantId, edge.getId(), attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("[{}][{}] Interrupted while delivering {} edge event(s)", tenantId, edge.getId(), edgeEvents.size());
                return false;
            } catch (Exception e) {
                // Not retried on purpose: a failure that is not an interruption would repeat on every attempt and
                // block this edge's downlink queue forever. Left uncommitted, so it is redelivered on reconnect.
                log.error("[{}][{}] Failed to process downlink messages", tenantId, edge.getId(), e);
                return false;
            }
            if (!sleepBeforeNextAttempt()) {
                return false;
            }
        }
        log.debug("[{}][{}] Session can no longer deliver {} edge event(s), offset is left uncommitted for redelivery",
                tenantId, edge.getId(), edgeEvents.size());
        return false;
    }

    /**
     * Holding a pack for longer than max.poll.interval.ms gets this consumer evicted from its group, after which the
     * pack is redelivered on the rejoin - recoverable, but the consumer goes quiet in the meantime, so the fact that a
     * sync (or high priority processing) is not letting the queue drain has to be visible in the log.
     *
     * @return the timestamp of the next warning
     */
    private long warnIfPackIsHeldForTooLong(int packSize, long startTs, long nextWarnTs) {
        long now = System.currentTimeMillis();
        if (now < nextWarnTs) {
            return nextWarnTs;
        }
        log.warn("[{}][{}] {} edge event(s) have not been delivered for {} ms, sync in progress = {}, high priority in progress = {}",
                tenantId, edge.getId(), packSize, now - startTs, isSyncInProgress(), isHighPriorityProcessing);
        return now + DELIVERY_WARN_INTERVAL_MS;
    }

    private boolean sleepBeforeNextAttempt() {
        try {
            Thread.sleep(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isReadyToProcessGeneralEvents() {
        return isConnected() && !isSyncInProgress() && !isHighPriorityProcessing;
    }

    @Override
    public ListenableFuture<Boolean> migrateEdgeEvents() throws Exception {
        return super.processEdgeEvents();
    }

    @Override
    public ListenableFuture<Boolean> processEdgeEvents() {
        if (!isReadyToProcessGeneralEvents()) {
            log.warn("[{}][{}] Session is not ready (connected={}, syncInProgress={}, highPriority={}), skip starting edge event consumer",
                    tenantId, edge != null ? edge.getId() : null, isConnected(), isSyncInProgress(), isHighPriorityProcessing);
            return Futures.immediateFuture(Boolean.FALSE);
        }
        if (consumer == null || (consumer.getConsumer() != null && consumer.getConsumer().isStopped())) {
            try {
                if (consumerExecutor != null && !consumerExecutor.isShutdown()) {
                    try {
                        consumerExecutor.shutdown();
                        awaitConsumerTermination();
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to shutdown previous consumer executor", tenantId, edge.getId(), e);
                    }
                }
                this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-event-consumer"));
                this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>builder()
                        .name("TB Edge events [" + edge.getId() + "]")
                        .msgPackProcessor(this::processMsgs)
                        .pollInterval(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval())
                        .consumerCreator(() -> tbCoreQueueFactory.createEdgeEventMsgConsumer(tenantId, edge.getId()))
                        .consumerExecutor(consumerExecutor)
                        .threadPrefix("edge-events-" + edge.getId())
                        .readinessCheck(this::isReadyToProcessGeneralEvents)
                        .build();
                consumer.subscribe();
                consumer.launch();
            } catch (Exception e) {
                destroy();
                log.warn("[{}][{}] Failed to start edge event consumer", sessionId, edge.getId(), e);
            }
        }
        return Futures.immediateFuture(Boolean.FALSE);
    }

    @Override
    public void processHighPriorityEvents() {
        isHighPriorityProcessing = true;
        try {
            super.processHighPriorityEvents();
        } finally {
            isHighPriorityProcessing = false;
        }
    }

    @Override
    public boolean destroy() {
        stopCurrentSendDownlinkMsgsTask(true);
        try {
            if (consumer != null) {
                log.info("[{}][{}] Stopping edge event consumer...", tenantId, edge != null ? edge.getId() : null);
                consumer.stop();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to stop edge event consumer", tenantId, edge.getId(), e);
            return false;
        }
        consumer = null;
        try {
            if (consumerExecutor != null && !consumerExecutor.isShutdown()) {
                consumerExecutor.shutdown();
                awaitConsumerTermination();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to shutdown edge event consumer executor", tenantId, edge.getId(), e);
            return false;
        }
        return true;
    }

    private void awaitConsumerTermination() {
        try {
            if (!consumerExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                // without this the thread is leaked for the whole remaining delivery-retry window (or forever, if the future is never completed).
                consumerExecutor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            log.warn("[{}][{}] Interrupted while awaiting consumer executor termination", tenantId, edge.getId());
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void cleanUp() {
        String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edge.getId()).getTopic();
        kafkaAdmin.deleteTopic(topic);
        kafkaAdmin.deleteConsumerGroup(topic);
    }

}
