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

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.PartitionedQueueResponseTemplate;
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
    private final EdqsExecutors edqsExecutors;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbKafkaConsumerStatsService consumerStatsService;
    private final TopicService topicService;
    private final StatsFactory statsFactory;

    private final AtomicInteger consumerCounter = new AtomicInteger();

    public KafkaEdqsQueueFactory(TbKafkaSettings kafkaSettings, TbKafkaTopicConfigs topicConfigs,
                                 EdqsConfig edqsConfig, EdqsExecutors edqsExecutors, TbServiceInfoProvider serviceInfoProvider,
                                 TbKafkaConsumerStatsService consumerStatsService, TopicService topicService,
                                 StatsFactory statsFactory) {
        this.edqsEventsAdmin = new TbKafkaAdmin(kafkaSettings, topicConfigs.getEdqsEventsConfigs());
        this.edqsRequestsAdmin = new TbKafkaAdmin(kafkaSettings, topicConfigs.getEdqsRequestsConfigs());
        this.edqsStateAdmin = new TbKafkaAdmin(kafkaSettings, topicConfigs.getEdqsStateConfigs());
        this.kafkaSettings = kafkaSettings;
        this.edqsConfig = edqsConfig;
        this.edqsExecutors = edqsExecutors;
        this.serviceInfoProvider = serviceInfoProvider;
        this.consumerStatsService = consumerStatsService;
        this.topicService = topicService;
        this.statsFactory = statsFactory;
    }

    @Override
    public TbKafkaConsumerTemplate<TbProtoQueueMsg<ToEdqsMsg>> createEdqsEventsConsumer() {
        return createEdqsMsgConsumer(edqsConfig.getEventsTopic(),
                "edqs-events-" + consumerCounter.getAndIncrement() + "-consumer-" + serviceInfoProvider.getServiceId(),
                null, // not using consumer group management, offsets from the edqs-events-to-backup-consumer-group are used (see KafkaEdqsStateService)
                false, edqsEventsAdmin);
    }

    @Override
    public TbKafkaConsumerTemplate<TbProtoQueueMsg<ToEdqsMsg>> createEdqsEventsToBackupConsumer() {
        return createEdqsMsgConsumer(edqsConfig.getEventsTopic(),
                "edqs-events-to-backup-consumer-" + serviceInfoProvider.getServiceId(),
                "edqs-events-to-backup-consumer-group",
                false, edqsEventsAdmin);
    }

    @Override
    public TbKafkaConsumerTemplate<TbProtoQueueMsg<ToEdqsMsg>> createEdqsStateConsumer() {
        return createEdqsMsgConsumer(edqsConfig.getStateTopic(),
                "edqs-state-" + consumerCounter.getAndIncrement() + "-consumer-" + serviceInfoProvider.getServiceId(),
                null, // not using consumer group management
                true, edqsStateAdmin);
    }

    public TbKafkaConsumerTemplate<TbProtoQueueMsg<ToEdqsMsg>> createEdqsMsgConsumer(String topic, String clientId, String group, boolean readFullAndStop, TbKafkaAdmin admin) {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<ToEdqsMsg>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(topic))
                .readFromBeginning(readFullAndStop)
                .stopWhenRead(readFullAndStop)
                .clientId(clientId)
                .groupId(topicService.buildTopicName(group))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToEdqsMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(admin)
                .statsService(consumerStatsService)
                .build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsStateProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<ToEdqsMsg>>builder()
                .clientId("edqs-state-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(edqsConfig.getStateTopic()))
                .settings(kafkaSettings)
                .admin(edqsStateAdmin)
                .build();
    }

    @Override
    public PartitionedQueueResponseTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> createEdqsResponseTemplate(TbQueueHandler<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> handler) {
        var responseProducer = TbKafkaProducerTemplate.<TbProtoQueueMsg<FromEdqsMsg>>builder()
                .settings(kafkaSettings)
                .clientId("edqs-response-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(edqsConfig.getResponsesTopic()))
                .admin(edqsRequestsAdmin)
                .build();
        return PartitionedQueueResponseTemplate.<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>>builder()
                .key("edqs")
                .handler(handler)
                .requestsTopic(topicService.buildTopicName(edqsConfig.getRequestsTopic()))
                .consumerCreator(tpi -> createEdqsMsgConsumer(edqsConfig.getRequestsTopic(),
                        "edqs-requests-consumer-" + serviceInfoProvider.getServiceId() + "-" + tpi.getPartition().orElse(999),
                        "edqs-requests-consumer-group",
                        false, edqsRequestsAdmin))
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
    public TbKafkaAdmin getEdqsQueueAdmin() {
        return edqsEventsAdmin;
    }

}
