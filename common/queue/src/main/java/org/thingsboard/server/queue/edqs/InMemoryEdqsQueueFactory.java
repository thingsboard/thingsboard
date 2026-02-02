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
package org.thingsboard.server.queue.edqs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.PartitionedQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;

@Component
@InMemoryEdqsComponent
@RequiredArgsConstructor
public class InMemoryEdqsQueueFactory implements EdqsQueueFactory {

    private final InMemoryStorage storage;
    private final EdqsConfig edqsConfig;
    private final EdqsExecutors edqsExecutors;
    private final StatsFactory statsFactory;
    private final TbQueueAdmin queueAdmin;

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsEventsConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, edqsConfig.getEventsTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsEventsToBackupConsumer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsStateConsumer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsStateProducer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PartitionedQueueResponseTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> createEdqsResponseTemplate(TbQueueHandler<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> handler) {
        TbQueueProducer<TbProtoQueueMsg<FromEdqsMsg>> responseProducer = new InMemoryTbQueueProducer<>(storage, edqsConfig.getResponsesTopic());
        return PartitionedQueueResponseTemplate.<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>>builder()
                .key("edqs")
                .handler(handler)
                .requestsTopic(edqsConfig.getRequestsTopic())
                .consumerCreator(tpi -> new InMemoryTbQueueConsumer<>(storage, edqsConfig.getRequestsTopic()))
                .responseProducer(responseProducer)
                .pollInterval(edqsConfig.getPollInterval())
                .requestTimeout(edqsConfig.getMaxRequestTimeout())
                .maxPendingRequests(edqsConfig.getMaxPendingRequests())
                .consumerExecutor(edqsExecutors.getConsumersExecutor())
                .callbackExecutor(edqsExecutors.getRequestExecutor())
                .consumerTaskExecutor(edqsExecutors.getConsumerTaskExecutor())
                .stats(statsFactory.createMessagesStats(StatsType.EDQS.getName()))
                .build();
    }

    @Override
    public TbQueueAdmin getEdqsQueueAdmin() {
        return queueAdmin;
    }

}
