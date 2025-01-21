/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
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

    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaTopicConfigs kafkaTopicConfigs;

    private volatile boolean isHighPriorityProcessing;

    private QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer;

    private ExecutorService consumerExecutor;

    public KafkaEdgeGrpcSession(EdgeContextComponent ctx, TopicService topicService, TbCoreQueueFactory tbCoreQueueFactory,
                                TbKafkaSettings kafkaSettings, TbKafkaTopicConfigs kafkaTopicConfigs, StreamObserver<ResponseMsg> outputStream,
                                BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener, BiConsumer<Edge, UUID> sessionCloseListener,
                                ScheduledExecutorService sendDownlinkExecutorService, int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        super(ctx, outputStream, sessionOpenListener, sessionCloseListener, sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
        this.topicService = topicService;
        this.tbCoreQueueFactory = tbCoreQueueFactory;
        this.kafkaSettings = kafkaSettings;
        this.kafkaTopicConfigs = kafkaTopicConfigs;
    }

    private void processMsgs(List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer) {
        log.trace("[{}][{}] starting processing edge events", tenantId, sessionId);
        if (isConnected() && !isSyncInProgress() && !isHighPriorityProcessing) {
            List<EdgeEvent> edgeEvents = new ArrayList<>();
            for (TbProtoQueueMsg<ToEdgeEventNotificationMsg> msg : msgs) {
                EdgeEvent edgeEvent = ProtoUtils.fromProto(msg.getValue().getEdgeEventMsg());
                edgeEvents.add(edgeEvent);
            }
            List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(edgeEvents);
            try {
                boolean isInterrupted = sendDownlinkMsgsPack(downlinkMsgsPack).get();
                if (isInterrupted) {
                    log.debug("[{}][{}][{}] Send downlink messages task was interrupted", tenantId, edge.getId(), sessionId);
                } else {
                    consumer.commit();
                }
            } catch (Exception e) {
                log.error("[{}] Failed to process all downlink messages", sessionId, e);
            }
        } else {
            try {
                Thread.sleep(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval());
            } catch (InterruptedException interruptedException) {
                log.trace("Failed to wait until the server has capacity to handle new requests", interruptedException);
            }
            log.trace("[{}][{}] edge is not connected or sync is not completed. Skipping iteration", tenantId, sessionId);
        }
    }

    @Override
    public ListenableFuture<Boolean> migrateEdgeEvents() throws Exception {
        return super.processEdgeEvents();
    }

    @Override
    public ListenableFuture<Boolean> processEdgeEvents() {
        if (consumer == null) {
            this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-event-consumer"));
            this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>builder()
                    .name("TB Edge events")
                    .msgPackProcessor(this::processMsgs)
                    .pollInterval(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval())
                    .consumerCreator(() -> tbCoreQueueFactory.createEdgeEventMsgConsumer(tenantId, edge.getId()))
                    .consumerExecutor(consumerExecutor)
                    .threadPrefix("edge-events")
                    .build();
            consumer.subscribe();
            consumer.launch();
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
    public void destroy() {
        consumer.stop();
        consumerExecutor.shutdown();
    }

    @Override
    public void cleanUp() {
        String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edge.getId()).getTopic();
        TbKafkaAdmin kafkaAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getEdgeEventConfigs());
        kafkaAdmin.deleteTopic(topic);
    }

}
