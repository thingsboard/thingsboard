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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaEdgeEventService implements EdgeEventService {

    private final RateLimitService rateLimitService;
    private final DataValidator<EdgeEvent> edgeEventValidator;
    @Lazy
    private final TbQueueProducerProvider producerProvider;
    @Lazy
    private final TopicService topicService;

    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        if (!rateLimitService.checkRateLimit(LimitedApi.EDGE_EVENTS, edgeEvent.getTenantId())) {
            throw new TbRateLimitsException(EntityType.TENANT);
        }
        if (!rateLimitService.checkRateLimit(LimitedApi.EDGE_EVENTS_PER_EDGE, edgeEvent.getTenantId(), edgeEvent.getEdgeId())) {
            throw new TbRateLimitsException(EntityType.EDGE);
        }
        edgeEventValidator.validate(edgeEvent, EdgeEvent::getTenantId);

        TopicPartitionInfo tpi = topicService.getEdgeEventNotificationsTopic(edgeEvent.getTenantId(), edgeEvent.getEdgeId());
        ToEdgeEventNotificationMsg msg = ToEdgeEventNotificationMsg.newBuilder().setEdgeEventMsg(ProtoUtils.toProto(edgeEvent)).build();
        producerProvider.getTbEdgeEventsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);

        return Futures.immediateFuture(null);
    }

    @Override
    public PageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return null;
    }

    @Override
    public void cleanupEvents(long ttl) {
    }

}
