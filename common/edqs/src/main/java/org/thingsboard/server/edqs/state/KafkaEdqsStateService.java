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
package org.thingsboard.server.edqs.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.edqs.processor.EdqsProcessor;
import org.thingsboard.server.edqs.processor.EdqsProducer;
import org.thingsboard.server.edqs.util.VersionsStore;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.common.state.KafkaQueueStateService;
import org.thingsboard.server.queue.common.state.QueueStateService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.edqs.KafkaEdqsComponent;
import org.thingsboard.server.queue.edqs.KafkaEdqsQueueFactory;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@KafkaEdqsComponent
@Slf4j
public class KafkaEdqsStateService implements EdqsStateService {

    private final EdqsConfig config;
    private final EdqsPartitionService partitionService;
    private final KafkaEdqsQueueFactory queueFactory;
    @Autowired
    @Lazy
    private EdqsProcessor edqsProcessor;

    private PartitionedQueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>> stateConsumer;
    private QueueStateService<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<ToEdqsMsg>> queueStateService;
    private QueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>> eventsToBackupConsumer;
    private EdqsProducer stateProducer;

    private final VersionsStore versionsStore = new VersionsStore();
    private final AtomicInteger stateReadCount = new AtomicInteger();
    private final AtomicInteger eventsReadCount = new AtomicInteger();
    private Boolean ready;

    @Override
    public void init(PartitionedQueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>> eventConsumer) {
        TbKafkaAdmin queueAdmin = queueFactory.getEdqsQueueAdmin();
        stateConsumer = PartitionedQueueConsumerManager.<TbProtoQueueMsg<ToEdqsMsg>>create()
                .queueKey(new QueueKey(ServiceType.EDQS, config.getStateTopic()))
                .topic(config.getStateTopic())
                .pollInterval(config.getPollInterval())
                .msgPackProcessor((msgs, consumer, config) -> {
                    for (TbProtoQueueMsg<ToEdqsMsg> queueMsg : msgs) {
                        try {
                            ToEdqsMsg msg = queueMsg.getValue();
                            edqsProcessor.process(msg, false);
                            if (stateReadCount.incrementAndGet() % 100000 == 0) {
                                log.info("[state] Processed {} msgs", stateReadCount.get());
                            }
                        } catch (Exception e) {
                            log.error("Failed to process message: {}", queueMsg, e);
                        }
                    }
                    consumer.commit();
                })
                .consumerCreator((config, tpi) -> queueFactory.createEdqsStateConsumer())
                .queueAdmin(queueAdmin)
                .consumerExecutor(eventConsumer.getConsumerExecutor())
                .taskExecutor(eventConsumer.getTaskExecutor())
                .scheduler(eventConsumer.getScheduler())
                .uncaughtErrorHandler(edqsProcessor.getErrorHandler())
                .build();

        TbKafkaConsumerTemplate<TbProtoQueueMsg<ToEdqsMsg>> eventsToBackupKafkaConsumer = queueFactory.createEdqsEventsToBackupConsumer();
        eventsToBackupConsumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdqsMsg>>builder()
                .name("edqs-events-to-backup-consumer")
                .pollInterval(config.getPollInterval())
                .msgPackProcessor((msgs, consumer) -> {
                    for (TbProtoQueueMsg<ToEdqsMsg> queueMsg : msgs) {
                        if (consumer.isStopped()) {
                            return;
                        }
                        try {
                            ToEdqsMsg msg = queueMsg.getValue();
                            log.trace("Processing message: {}", msg);

                            if (msg.hasEventMsg()) {
                                EdqsEventMsg eventMsg = msg.getEventMsg();
                                String key = eventMsg.getKey();
                                int count = eventsReadCount.incrementAndGet();
                                if (count % 100000 == 0) {
                                    log.info("[events-to-backup] Processed {} msgs", count);
                                }
                                if (eventMsg.hasVersion()) {
                                    if (!versionsStore.isNew(key, eventMsg.getVersion())) {
                                        continue;
                                    }
                                }

                                TenantId tenantId = getTenantId(msg);
                                ObjectType objectType = ObjectType.valueOf(eventMsg.getObjectType());
                                EdqsEventType eventType = EdqsEventType.valueOf(eventMsg.getEventType());
                                log.trace("[{}] Saving to backup [{}] [{}] [{}]", tenantId, objectType, eventType, key);
                                stateProducer.send(tenantId, objectType, key, msg);
                            }
                        } catch (Throwable t) {
                            log.error("Failed to process message: {}", queueMsg, t);
                        }
                    }
                    consumer.commit();
                })
                .consumerCreator(() -> eventsToBackupKafkaConsumer)
                .consumerExecutor(eventConsumer.getConsumerExecutor())
                .threadPrefix("edqs-events-to-backup")
                .build();

        stateProducer = EdqsProducer.builder()
                .producer(queueFactory.createEdqsStateProducer())
                .partitionService(partitionService)
                .build();

        queueStateService = KafkaQueueStateService.<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<ToEdqsMsg>>builder()
                .eventConsumer(eventConsumer)
                .stateConsumer(stateConsumer)
                .eventsStartOffsetsProvider(() -> {
                    // taking start offsets for events topics from the events-to-backup consumer group,
                    // since eventConsumer doesn't use consumer group management and thus offset tracking
                    // (because we need to be able to consume the same topic-partition by multiple instances)
                    Map<String, Long> offsets = new HashMap<>();
                    try {
                        queueAdmin.getConsumerGroupOffsets(eventsToBackupKafkaConsumer.getGroupId())
                                .forEach((topicPartition, offsetAndMetadata) -> {
                                    offsets.put(topicPartition.topic(), offsetAndMetadata.offset());
                                });
                    } catch (Exception e) {
                        log.error("Failed to get consumer group offsets for {}", eventsToBackupKafkaConsumer.getGroupId(), e);
                    }
                    return offsets;
                })
                .build();
    }

    @Override
    public void process(Set<TopicPartitionInfo> partitions) {
        if (queueStateService.getPartitions().isEmpty()) {
            Set<TopicPartitionInfo> allPartitions = IntStream.range(0, config.getPartitions())
                    .mapToObj(partition -> TopicPartitionInfo.builder()
                            .topic(config.getEventsTopic())
                            .partition(partition)
                            .build())
                    .collect(Collectors.toSet());
            eventsToBackupConsumer.subscribe(allPartitions);
            eventsToBackupConsumer.launch();
        }
        queueStateService.update(new QueueKey(ServiceType.EDQS), partitions);
    }

    @Override
    public void save(TenantId tenantId, ObjectType type, String key, EdqsEventType eventType, ToEdqsMsg msg) {
        // do nothing here, backup is done by events consumer
    }

    @Override
    public boolean isReady() {
        if (ready == null) {
            Set<TopicPartitionInfo> partitionsInProgress = queueStateService.getPartitionsInProgress();
            if (partitionsInProgress != null && partitionsInProgress.isEmpty()) {
                ready = true; // once true - always true, not to change readiness status on each repartitioning
            }
        }
        return ready != null && ready;
    }

    private TenantId getTenantId(ToEdqsMsg edqsMsg) {
        return TenantId.fromUUID(new UUID(edqsMsg.getTenantIdMSB(), edqsMsg.getTenantIdLSB()));
    }

    @Override
    public void stop() {
        stateConsumer.stop();
        stateConsumer.awaitStop();
        eventsToBackupConsumer.stop();
        stateProducer.stop();
    }

}
