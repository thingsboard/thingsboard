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
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

@Slf4j
public class KafkaEdgeGrpcSession extends AbstractEdgeGrpcSession<KafkaEdgeGrpcSession> {

    private TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> edgeEventsConsumer;

    public KafkaEdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream,
                                BiConsumer<EdgeId, KafkaEdgeGrpcSession> sessionOpenListener,
                                BiConsumer<Edge, UUID> sessionCloseListener,
                                ScheduledExecutorService sendDownlinkExecutorService,
                                int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        super(ctx, outputStream, sessionOpenListener, sessionCloseListener, sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
    }

    @Override
    protected ListenableFuture<Boolean> processEdgeEvents() {
        log.trace("[{}][{}] starting processing edge events", this.tenantId, this.sessionId);
        if (isConnected() && isSyncCompleted()) {

            if (!highPriorityQueue.isEmpty()) {
                processHighPriorityEvents();
            } else {
                return processKafkaEvents();
            }
        } else {
            log.trace("[{}][{}] edge is not connected or sync is not completed. Skipping iteration", tenantId, sessionId);
        }
        return null;
    }

    private ListenableFuture<Boolean> processKafkaEvents() {
        List<EdgeEvent> edgeEvents = new ArrayList<>();
        try {
            edgeEventsConsumer.subscribe();
            List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> messages = edgeEventsConsumer.poll(100);

            if (messages.isEmpty()) {
                return Futures.immediateFuture(Boolean.FALSE);
            }

            for (TbProtoQueueMsg<ToEdgeEventNotificationMsg> msg : messages) {
                EdgeEvent edgeEvent = ProtoUtils.fromProto(msg.getValue().getEdgeEventMsg());
                edgeEvents.add(edgeEvent);
            }

            List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(edgeEvents);
            sendDownlinkMsgsPack(downlinkMsgsPack).get();
            edgeEventsConsumer.commit();
            return Futures.immediateFuture(Boolean.TRUE);
        } catch (Exception e) {
            log.error("[{}][{}] Error occurred while polling edge events from Kafka: {}", tenantId, edge.getId(), e.getMessage());
            return Futures.immediateFailedFuture(e);
        }
    }

    protected void initConsumer(TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToEdgeEventNotificationMsg>> edgeEventsConsumer) {
        this.edgeEventsConsumer = edgeEventsConsumer;
    }

    @PreDestroy
    private void destroy() {
        if (edgeEventsConsumer != null) {
            edgeEventsConsumer.unsubscribe();
        }
    }

    public void stopConsumer() {
        if (edgeEventsConsumer != null) {
            edgeEventsConsumer.stop();
        }
    }

}
