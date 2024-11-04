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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
public class KafkaEdgeGrpcSession extends AbstractEdgeGrpcSession<KafkaEdgeGrpcSession> {

    private ExecutorService consumerExecutor;

    private QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer;

    public KafkaEdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream,
                                BiConsumer<EdgeId, KafkaEdgeGrpcSession> sessionOpenListener,
                                BiConsumer<Edge, UUID> sessionCloseListener,
                                ScheduledExecutorService sendDownlinkExecutorService,
                                int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        super(ctx, outputStream, sessionOpenListener, sessionCloseListener, sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
    }

    protected void initConsumer(Supplier<TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>> edgeEventsConsumer, long schedulerPoolSize) {
        this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-event-consumer"));
        this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>builder()
                .name("TB Edge events")
                .msgPackProcessor(this::processMsgs)
                .pollInterval(schedulerPoolSize)
                .consumerCreator(edgeEventsConsumer)
                .consumerExecutor(consumerExecutor)
                .threadPrefix("edge-events")
                .build();
    }

    private void processMsgs(List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer) {
        log.trace("[{}][{}] starting processing edge events", tenantId, sessionId);
        if (isConnected() && isSyncCompleted()) {

            if (!highPriorityQueue.isEmpty()) {
                processHighPriorityEvents();
            } else {
                List<EdgeEvent> edgeEvents = new ArrayList<>();
                for (TbProtoQueueMsg<ToEdgeEventNotificationMsg> msg : msgs) {
                    EdgeEvent edgeEvent = ProtoUtils.fromProto(msg.getValue().getEdgeEventMsg());
                    edgeEvents.add(edgeEvent);
                }
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(edgeEvents);
                Futures.addCallback(sendDownlinkMsgsPack(downlinkMsgsPack), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Boolean isInterrupted) {
                        if (Boolean.TRUE.equals(isInterrupted)) {
                            log.debug("[{}][{}][{}] Send downlink messages task was interrupted", tenantId, edge.getId(), sessionId);
                        } else {
                            consumerExecutor.submit(consumer::commit);
                        }
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to send downlink msgs pack", sessionId, t);
                    }
                }, ctx.getGrpcCallbackExecutorService());
            }
        } else {
            log.trace("[{}][{}] edge is not connected or sync is not completed. Skipping iteration", tenantId, sessionId);
        }
    }

    public void startConsumers() {
        consumer.subscribe();
        consumer.launch();
    }

    @PreDestroy
    private void destroy() {
        consumer.stop();
        consumerExecutor.shutdown();
    }

    public void stopConsumer() {
        consumer.stop();
        consumerExecutor.shutdown();
    }

}
