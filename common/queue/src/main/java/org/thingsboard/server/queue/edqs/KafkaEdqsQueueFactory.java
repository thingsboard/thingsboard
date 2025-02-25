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
package org.thingsboard.server.queue.edqs;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@KafkaEdqsComponent
public class KafkaEdqsQueueFactory implements EdqsQueueFactory {

    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaAdmin edqsEventsAdmin;
    private final TbKafkaAdmin edqsRequestsAdmin;
    private final TbKafkaAdmin edqsStateAdmin;
    private final EdqsConfig edqsConfig;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbKafkaConsumerStatsService consumerStatsService;
    private final TopicService topicService;
    private final StatsFactory statsFactory;

    private final AtomicInteger consumerCounter = new AtomicInteger();

    public KafkaEdqsQueueFactory(TbKafkaSettings kafkaSettings, TbKafkaTopicConfigs topicConfigs,
                                 EdqsConfig edqsConfig, TbServiceInfoProvider serviceInfoProvider,
                                 TbKafkaConsumerStatsService consumerStatsService, TopicService topicService,
                                 StatsFactory statsFactory) {
        this.edqsEventsAdmin = new TbKafkaAdmin(kafkaSettings, topicConfigs.getEdqsEventsConfigs());
        this.edqsRequestsAdmin = new TbKafkaAdmin(kafkaSettings, topicConfigs.getEdqsRequestsConfigs());
        this.edqsStateAdmin = new TbKafkaAdmin(kafkaSettings, topicConfigs.getEdqsStateConfigs());
        this.kafkaSettings = kafkaSettings;
        this.edqsConfig = edqsConfig;
        this.serviceInfoProvider = serviceInfoProvider;
        this.consumerStatsService = consumerStatsService;
        this.topicService = topicService;
        this.statsFactory = statsFactory;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsMsgConsumer(EdqsQueue queue) {
        String consumerGroup = "edqs-" + queue.name().toLowerCase() + "-consumer-group-" + serviceInfoProvider.getServiceId();
        return createEdqsMsgConsumer(queue, consumerGroup);
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsMsgConsumer(EdqsQueue queue, String group) {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<ToEdqsMsg>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(queue.getTopic()))
                .readFromBeginning(queue.isReadFromBeginning())
                .stopWhenRead(queue.isStopWhenRead())
                .clientId("edqs-" + queue.name().toLowerCase() + "-" + consumerCounter.getAndIncrement() + "-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName(group))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToEdqsMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(queue == EdqsQueue.STATE ? edqsStateAdmin : edqsEventsAdmin)
                .statsService(consumerStatsService)
                .build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsMsgProducer(EdqsQueue queue) {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<ToEdqsMsg>>builder()
                .clientId("edqs-" + queue.name().toLowerCase() + "-producer-" + serviceInfoProvider.getServiceId())
                .settings(kafkaSettings)
                .admin(queue == EdqsQueue.STATE ? edqsStateAdmin : edqsEventsAdmin)
                .build();
    }

    @Override
    public TbQueueResponseTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> createEdqsResponseTemplate() {
        String requestsConsumerGroup = "edqs-requests-consumer-group-" + edqsConfig.getLabel();
        var requestConsumer = TbKafkaConsumerTemplate.<TbProtoQueueMsg<TransportProtos.ToEdqsMsg>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(edqsConfig.getRequestsTopic()))
                .clientId("edqs-requests-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName(requestsConsumerGroup))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToEdqsMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(edqsRequestsAdmin)
                .statsService(consumerStatsService);
        var responseProducer = TbKafkaProducerTemplate.<TbProtoQueueMsg<FromEdqsMsg>>builder()
                .settings(kafkaSettings)
                .clientId("edqs-response-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(edqsConfig.getResponsesTopic()))
                .admin(edqsRequestsAdmin);
        return DefaultTbQueueResponseTemplate.<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>>builder()
                .requestTemplate(requestConsumer.build())
                .responseTemplate(responseProducer.build())
                .maxPendingRequests(edqsConfig.getMaxPendingRequests())
                .requestTimeout(edqsConfig.getMaxRequestTimeout())
                .pollInterval(edqsConfig.getPollInterval())
                .stats(statsFactory.createMessagesStats(StatsType.EDQS.getName()))
                .executor(ThingsBoardExecutors.newWorkStealingPool(5, "edqs"))
                .build();
    }

}
