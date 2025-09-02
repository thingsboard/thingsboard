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
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
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
        if (!isConnected() || isSyncInProgress() || isHighPriorityProcessing) {
            log.debug("[{}][{}] edge not connected, edge sync is not completed or high priority processing in progress, " +
                      "connected = {}, sync in progress = {}, high priority in progress = {}. Skipping iteration",
                    tenantId, edge.getId(), isConnected(), isSyncInProgress(), isHighPriorityProcessing);
            return;
        }
        List<EdgeEvent> edgeEvents = new ArrayList<>();
        for (TbProtoQueueMsg<ToEdgeEventNotificationMsg> msg : msgs) {
            EdgeEvent edgeEvent = ProtoUtils.fromProto(msg.getValue().getEdgeEventMsg());
            edgeEvents.add(edgeEvent);
        }
        List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(edgeEvents);
        try {
            boolean isInterrupted = sendDownlinkMsgsPack(downlinkMsgsPack).get();
            if (isInterrupted) {
                log.debug("[{}][{}] Send downlink messages task was interrupted", tenantId, edge.getId());
            } else {
                consumer.commit();
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process downlink messages", tenantId, edge.getId(), e);
        }
    }

    @Override
    public ListenableFuture<Boolean> migrateEdgeEvents() throws Exception {
        return super.processEdgeEvents();
    }

    @Override
    public ListenableFuture<Boolean> processEdgeEvents() {
        if (consumer == null || (consumer.getConsumer() != null && consumer.getConsumer().isStopped())) {
            try {
                this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-event-consumer"));
                this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>builder()
                        .name("TB Edge events [" + edge.getId() + "]")
                        .msgPackProcessor(this::processMsgs)
                        .pollInterval(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval())
                        .consumerCreator(() -> tbCoreQueueFactory.createEdgeEventMsgConsumer(tenantId, edge.getId()))
                        .consumerExecutor(consumerExecutor)
                        .threadPrefix("edge-events-" + edge.getId())
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
        super.processHighPriorityEvents();
        isHighPriorityProcessing = false;
    }

    @Override
    public boolean destroy() {
        try {
            if (consumer != null) {
                consumer.stop();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to stop edge event consumer", tenantId, edge.getId(), e);
            return false;
        }
        consumer = null;
        try {
            if (consumerExecutor != null) {
                consumerExecutor.shutdown();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to shutdown consumer executor", tenantId, edge.getId(), e);
            return false;
        }
        return true;
    }

    @Override
    public void cleanUp() {
        String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edge.getId()).getTopic();
        kafkaAdmin.deleteTopic(topic);
        kafkaAdmin.deleteConsumerGroup(topic);
    }

}
