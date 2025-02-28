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
import org.thingsboard.server.queue.common.consumer.QueueStateService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.edqs.EdqsQueue;
import org.thingsboard.server.queue.edqs.EdqsQueueFactory;
import org.thingsboard.server.queue.edqs.KafkaEdqsComponent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@KafkaEdqsComponent
@Slf4j
public class KafkaEdqsStateService implements EdqsStateService {

    private final EdqsConfig config;
    private final EdqsPartitionService partitionService;
    private final EdqsQueueFactory queueFactory;
    private final TopicService topicService;
    @Autowired @Lazy
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
        stateConsumer = PartitionedQueueConsumerManager.<TbProtoQueueMsg<ToEdqsMsg>>create()
                .queueKey(new QueueKey(ServiceType.EDQS, EdqsQueue.STATE.getTopic()))
                .topic(EdqsQueue.STATE.getTopic())
                .pollInterval(config.getPollInterval())
                .msgPackProcessor((msgs, consumer, config) -> {
                    for (TbProtoQueueMsg<ToEdqsMsg> queueMsg : msgs) {
                        try {
                            ToEdqsMsg msg = queueMsg.getValue();
                            edqsProcessor.process(msg, EdqsQueue.STATE);
                            if (stateReadCount.incrementAndGet() % 100000 == 0) {
                                log.info("[state] Processed {} msgs", stateReadCount.get());
                            }
                        } catch (Exception e) {
                            log.error("Failed to process message: {}", queueMsg, e);
                        }
                    }
                    consumer.commit();
                })
                .consumerCreator((config, partitionId) -> queueFactory.createEdqsMsgConsumer(EdqsQueue.STATE))
                .consumerExecutor(eventConsumer.getConsumerExecutor())
                .taskExecutor(eventConsumer.getTaskExecutor())
                .scheduler(eventConsumer.getScheduler())
                .uncaughtErrorHandler(edqsProcessor.getErrorHandler())
                .build();
        queueStateService = new QueueStateService<>();
        queueStateService.init(stateConsumer, eventConsumer);

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
                .consumerCreator(() -> queueFactory.createEdqsMsgConsumer(EdqsQueue.EVENTS, "events-to-backup-consumer-group")) // shared by all instances consumer group
                .consumerExecutor(eventConsumer.getConsumerExecutor())
                .threadPrefix("edqs-events-to-backup")
                .build();

        stateProducer = EdqsProducer.builder()
                .queue(EdqsQueue.STATE)
                .partitionService(partitionService)
                .topicService(topicService)
                .producer(queueFactory.createEdqsMsgProducer(EdqsQueue.STATE))
                .build();
    }

    @Override
    public void process(Set<TopicPartitionInfo> partitions) {
        if (queueStateService.getPartitions() == null) {
            eventsToBackupConsumer.subscribe();
            eventsToBackupConsumer.launch();
        }
        queueStateService.update(partitions);
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
