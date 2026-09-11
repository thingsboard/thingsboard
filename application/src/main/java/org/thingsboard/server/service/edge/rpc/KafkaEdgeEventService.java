// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.edge.BaseEdgeEventService;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.dao.edge.stats.EdgeStatsKey;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaEdgeEventService extends BaseEdgeEventService {

    private final TopicService topicService;
    private final TbQueueProducerProvider producerProvider;
    private final Optional<EdgeStatsCounterService> statsCounterService;

    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        validateEdgeEvent(edgeEvent);

        TopicPartitionInfo tpi = topicService.getEdgeEventNotificationsTopic(edgeEvent.getTenantId(), edgeEvent.getEdgeId());
        ToEdgeEventNotificationMsg msg = ToEdgeEventNotificationMsg.newBuilder().setEdgeEventMsg(ProtoUtils.toProto(edgeEvent)).build();
        producerProvider.getTbEdgeEventsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);
        statsCounterService.ifPresent(statsCounterService -> statsCounterService.recordEvent(EdgeStatsKey.DOWNLINK_MSGS_ADDED, edgeEvent.getTenantId(), edgeEvent.getEdgeId(), 1));
        return Futures.immediateFuture(null);
    }

}
