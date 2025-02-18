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
package org.thingsboard.server.service.cf.ctx.state;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.service.cf.AbstractCalculatedFieldStateService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.queue.DefaultTbCalculatedFieldConsumerService.CalculatedFieldQueueConfig;
import org.thingsboard.server.service.queue.consumer.MainQueueConsumerManager;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCalculatedFieldStateService extends AbstractCalculatedFieldStateService {

    private final TbRuleEngineQueueFactory queueFactory;
    private final PartitionService partitionService;

    @Value("${queue.calculated_fields.poll_interval:25}")
    private long pollInterval;
    @Value("${queue.calculated_fields.consumer_per_partition:true}")
    private boolean consumerPerPartition;

    private MainQueueConsumerManager<TbProtoQueueMsg<CalculatedFieldStateProto>, CalculatedFieldQueueConfig> stateConsumer;
    private TbKafkaProducerTemplate<TbProtoQueueMsg<CalculatedFieldStateProto>> stateProducer;

    protected ExecutorService consumersExecutor;
    protected ExecutorService mgmtExecutor;
    protected ScheduledExecutorService scheduler;

    private final AtomicInteger counter = new AtomicInteger();

    @PostConstruct
    private void init() {
        this.consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("cf-state-consumer"));
        this.mgmtExecutor = ThingsBoardExecutors.newWorkStealingPool(Math.max(Runtime.getRuntime().availableProcessors(), 4), "cf-state-mgmt");
        this.scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("cf-state-consumer-scheduler");

        this.stateConsumer = MainQueueConsumerManager.<TbProtoQueueMsg<CalculatedFieldStateProto>, CalculatedFieldQueueConfig>builder()
                .queueKey(QueueKey.CF_STATES)
                .config(CalculatedFieldQueueConfig.of(consumerPerPartition, (int) pollInterval))
                .msgPackProcessor((msgs, consumer, config) -> {
                    for (TbProtoQueueMsg<CalculatedFieldStateProto> msg : msgs) {
                        try {
                            processRestoredState(msg.getValue());
                        } catch (Throwable t) {
                            log.error("Failed to process state message: {}", msg, t);
                        }

                        int processedMsgCount = counter.incrementAndGet();
                        if (processedMsgCount % 10000 == 0) {
                            log.info("Processed {} calculated field state msgs", processedMsgCount);
                        }
                    }
                })
                .consumerCreator((config, partitionId) -> queueFactory.createCalculatedFieldStateConsumer())
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .build();
        this.stateProducer = (TbKafkaProducerTemplate<TbProtoQueueMsg<CalculatedFieldStateProto>>) queueFactory.createCalculatedFieldStateProducer();
    }

    @Override
    protected void doPersist(CalculatedFieldEntityCtxId stateId, CalculatedFieldStateProto stateMsgProto, TbCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(QueueKey.CF_STATES, stateId.entityId());
        stateProducer.send(tpi, stateId.toKey(), new TbProtoQueueMsg<>(stateId.entityId().getId(), stateMsgProto), new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    @Override
    protected void doRemove(CalculatedFieldEntityCtxId stateId, TbCallback callback) {
        //TODO: vklimov
    }

    @Override
    public void restore(Set<TopicPartitionInfo> partitions) {
        partitions = partitions.stream().map(tpi -> tpi.newByTopic(partitionService.getTopic(QueueKey.CF_STATES))).collect(Collectors.toSet());
        log.info("Restoring calculated field states for partitions: {}", partitions.stream().map(TopicPartitionInfo::getFullTopicName).toList());
        long startTs = System.currentTimeMillis();
        counter.set(0);

        stateConsumer.doUpdate(partitions); // calling blocking doUpdate instead of update
        stateConsumer.awaitStop(0);// consumers should stop on their own because stopWhenRead is true, we just need to wait

        log.info("Restored {} calculated field states in {} ms", counter.get(), System.currentTimeMillis() - startTs);
    }

    @PreDestroy
    private void preDestroy() {
        stateConsumer.stop();
        stateConsumer.awaitStop();
        stateProducer.stop();

        consumersExecutor.shutdownNow();
        mgmtExecutor.shutdownNow();
        scheduler.shutdownNow();
    }

}
