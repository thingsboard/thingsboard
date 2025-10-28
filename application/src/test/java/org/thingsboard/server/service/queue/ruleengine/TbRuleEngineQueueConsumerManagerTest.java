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
package org.thingsboard.server.service.queue.ruleengine;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.KafkaMonolithQueueFactory;
import org.thingsboard.server.queue.provider.KafkaTbRuleEngineQueueFactory;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContextFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
public class TbRuleEngineQueueConsumerManagerTest {

    @Mock
    private ActorSystemContext actorContext;
    @Mock
    private StatsFactory statsFactory;
    @Mock
    private TbRuleEngineQueueFactory queueFactory;
    @Mock
    private RuleEngineStatisticsService statisticsService;
    @Mock
    private TbServiceInfoProvider serviceInfoProvider;
    @Mock
    private PartitionService partitionService;
    @Mock
    private TbQueueProducerProvider producerProvider;
    @Mock
    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> ruleEngineMsgProducer;
    @Mock
    private TbQueueAdmin queueAdmin;
    private TbRuleEngineConsumerContext ruleEngineConsumerContext;
    private ExecutorService consumersExecutor;
    private ScheduledExecutorService scheduler;
    private ExecutorService mgmtExecutor;

    private TbRuleEngineQueueConsumerManager consumerManager;
    private Queue queue;

    private Set<TestConsumer> consumers;
    private boolean generateQueueMsgs;
    private AtomicInteger totalConsumedMsgs;
    private AtomicInteger totalProcessedMsgs;

    @BeforeEach
    public void beforeEach() {
        ruleEngineConsumerContext = new TbRuleEngineConsumerContext(
                actorContext, statsFactory, spy(new TbRuleEngineSubmitStrategyFactory()),
                spy(new TbRuleEngineProcessingStrategyFactory()), queueFactory, statisticsService,
                serviceInfoProvider, partitionService, producerProvider, queueAdmin
        );
        consumers = ConcurrentHashMap.newKeySet();
        generateQueueMsgs = true;
        totalConsumedMsgs = new AtomicInteger();
        totalProcessedMsgs = new AtomicInteger();
        doAnswer(inv -> {
            QueueToRuleEngineMsg msg = inv.getArgument(0);
            msg.getMsg().getCallback().onSuccess();
            totalProcessedMsgs.incrementAndGet();
            log.trace("totalProcessedMsgs = {}", totalProcessedMsgs);
            return null;
        }).when(actorContext).tell(any());

        when(producerProvider.getRuleEngineMsgProducer()).thenReturn(ruleEngineMsgProducer);
        consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("tb-rule-engine-consumer"));
        mgmtExecutor = ThingsBoardExecutors.newWorkStealingPool(3, "tb-rule-engine-mgmt");
        scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("tb-rule-engine-consumer-scheduler");
        ruleEngineConsumerContext.setTopicDeletionDelayInSec(5);

        queue = new Queue();
        queue.setName("Test");
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setId(new QueueId(UUID.randomUUID()));
        queue.setTopic("tb_test");
        queue.setPartitions(10);
        queue.setConsumerPerPartition(true);
        queue.setPollInterval(250);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.BURST);
        submitStrategy.setBatchSize(200);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES_AND_TIMED_OUT);
        processingStrategy.setRetries(0);
        queue.setProcessingStrategy(processingStrategy);

        doAnswer(i -> {
            TestConsumer consumer = spy(new TestConsumer(queue.getTopic()));
            if (generateQueueMsgs) {
                consumer.setUpTestMsg();
            }
            consumers.add(consumer);
            return consumer;
        }).when(queueFactory).createToRuleEngineMsgConsumer(any(), any());

        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queue);
        consumerManager = TbRuleEngineQueueConsumerManager.create()
                .ctx(ruleEngineConsumerContext)
                .queueKey(queueKey)
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .packProcessingContextFactory(new TbMsgPackProcessingContextFactory.DefaultTbMsgPackProcessingContextFactory())
                .build();
    }

    @AfterEach
    public void afterEach() {
        consumerManager.stop();
        consumerManager.awaitStop();

        consumersExecutor.shutdownNow();
        scheduler.shutdownNow();
        mgmtExecutor.shutdownNow();

        if (generateQueueMsgs) {
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        log.debug("totalConsumedMsgs = {}, totalProcessedMsgs = {}", totalConsumedMsgs.get(), totalProcessedMsgs.get());
                        assertThat(totalProcessedMsgs.get()).isEqualTo(totalConsumedMsgs.get());
                    });
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {KafkaMonolithQueueFactory.class, KafkaTbRuleEngineQueueFactory.class})
    public void testUnsupported_createToRuleEngineMsgConsumer_KafkaTbRuleEngineQueueFactory(Class<TbRuleEngineQueueFactory> factoryClass) {
        // obsolete, but need to pass the afterEach
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);

        var factory = mock(factoryClass);
        willCallRealMethod().given(factory).createToRuleEngineMsgConsumer(any());
        assertThatThrownBy(() -> factory.createToRuleEngineMsgConsumer(mock(Queue.class)))
                .isInstanceOf(UnsupportedOperationException.class);

    }

    @Test
    public void testInit_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);

        Set<TopicPartitionInfo> partitions = createTpis(2, 3, 4);
        consumerManager.update(partitions);

        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> consumers.size() == 3);
        for (TopicPartitionInfo partition : partitions) {
            TestConsumer consumer = getConsumer(partition);
            verifySubscribedAndLaunched(consumer, Set.of(partition));
        }
    }

    @Test
    public void testInit_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);

        Set<TopicPartitionInfo> partitions = createTpis(2, 3, 4);
        consumerManager.update(partitions);

        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> consumers.size() == 1);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);
    }

    @Test
    public void testPartitionsUpdate_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);

        Set<TopicPartitionInfo> partitions = Collections.emptySet();
        consumerManager.update(partitions);
        verify(queueFactory, after(1000).never()).createToRuleEngineMsgConsumer(any(), any());
        verify(queueFactory, never()).createToRuleEngineMsgConsumer(any());

        partitions = createTpis(1);
        consumerManager.update(partitions);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);

        partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        verifySubscribedAndLaunched(consumer, partitions);

        partitions = createTpis(4, 5, 6);
        consumerManager.update(partitions);
        verifySubscribedAndLaunched(consumer, partitions);

        partitions = Collections.emptySet();
        consumerManager.update(partitions);
        verifyUnsubscribedAndStopped(consumer);

        partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);
    }

    @Test
    public void testPartitionsUpdate_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);

        consumerManager.update(Collections.emptySet());
        verify(queueFactory, after(1000).never()).createToRuleEngineMsgConsumer(any(), any());
        verify(queueFactory, never()).createToRuleEngineMsgConsumer(any());

        consumerManager.update(createTpis(1));
        TestConsumer consumer1 = getConsumer(1);
        verifySubscribedAndLaunched(consumer1, 1);

        consumerManager.update(createTpis(1, 2, 3));
        TestConsumer consumer2 = getConsumer(2);
        TestConsumer consumer3 = getConsumer(3);
        verifySubscribedAndLaunched(consumer2, 2);
        verifySubscribedAndLaunched(consumer3, 3);
        verifyNotTouched(consumer1);

        consumerManager.update(createTpis(3, 4, 5));
        TestConsumer consumer4 = getConsumer(4);
        TestConsumer consumer5 = getConsumer(5);
        verifySubscribedAndLaunched(consumer4, 4);
        verifySubscribedAndLaunched(consumer5, 5);
        verifyUnsubscribedAndStopped(consumer1);
        verifyUnsubscribedAndStopped(consumer2);
        verifyNotTouched(consumer3);

        consumerManager.update(Collections.emptySet());
        verifyUnsubscribedAndStopped(consumer3);
        verifyUnsubscribedAndStopped(consumer4);
        verifyUnsubscribedAndStopped(consumer5);

        consumerManager.update(createTpis(1, 2, 3));
        consumer1 = getConsumer(1);
        consumer2 = getConsumer(2);
        consumer3 = getConsumer(3);
        verifySubscribedAndLaunched(consumer1, 1);
        verifySubscribedAndLaunched(consumer2, 2);
        verifySubscribedAndLaunched(consumer3, 3);
    }

    @Test
    public void testConfigUpdate_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);

        Queue newConfig = JacksonUtil.clone(queue);
        newConfig.setPollInterval(queue.getPollInterval() / 2);
        newConfig.setPartitions(queue.getPartitions() / 2);
        newConfig.setPackProcessingTimeout(queue.getPackProcessingTimeout() * 2);
        newConfig.getSubmitStrategy().setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        newConfig.getProcessingStrategy().setType(ProcessingStrategyType.RETRY_ALL);
        consumerManager.update(newConfig);

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(consumer, atLeastOnce()).poll(eq((long) newConfig.getPollInterval()));
                    verify(ruleEngineConsumerContext.getSubmitStrategyFactory(), atLeastOnce()).newInstance(any(), eq(newConfig.getSubmitStrategy()));
                    verify(ruleEngineConsumerContext.getProcessingStrategyFactory(), atLeastOnce()).newInstance(any(), eq(newConfig.getProcessingStrategy()));
                });
    }

    @Test
    public void testConfigUpdate_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        TestConsumer consumer1 = getConsumer(1);
        TestConsumer consumer2 = getConsumer(2);
        TestConsumer consumer3 = getConsumer(3);
        verifySubscribedAndLaunched(consumer1, 1);
        verifySubscribedAndLaunched(consumer2, 2);
        verifySubscribedAndLaunched(consumer3, 3);

        Queue newConfig = JacksonUtil.clone(queue);
        newConfig.setPollInterval(queue.getPollInterval() / 2);
        newConfig.setPartitions(queue.getPartitions() / 2);
        newConfig.setPackProcessingTimeout(queue.getPackProcessingTimeout() * 2);
        newConfig.getSubmitStrategy().setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        newConfig.getProcessingStrategy().setType(ProcessingStrategyType.RETRY_ALL);
        consumerManager.update(newConfig);

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(consumer1, atLeastOnce()).poll(eq((long) newConfig.getPollInterval()));
                    verify(consumer2, atLeastOnce()).poll(eq((long) newConfig.getPollInterval()));
                    verify(consumer3, atLeastOnce()).poll(eq((long) newConfig.getPollInterval()));
                });
        verifyNotTouched(consumer1);
        verifyNotTouched(consumer2);
        verifyNotTouched(consumer3);
    }

    @Test
    public void testConfigUpdate_fromSingleToConsumerPerPartition() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);

        Queue newConfig = JacksonUtil.clone(queue);
        newConfig.setConsumerPerPartition(true);
        consumerManager.update(newConfig);

        verifyUnsubscribedAndStopped(consumer);
        verifySubscribedAndLaunched(getConsumer(1), 1);
        verifySubscribedAndLaunched(getConsumer(2), 2);
        verifySubscribedAndLaunched(getConsumer(3), 3);
    }

    @Test
    public void testConfigUpdate_fromConsumerPerPartitionToSingle() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        TestConsumer consumer1 = getConsumer(1);
        TestConsumer consumer2 = getConsumer(2);
        TestConsumer consumer3 = getConsumer(3);
        verifySubscribedAndLaunched(consumer1, 1);
        verifySubscribedAndLaunched(consumer2, 2);
        verifySubscribedAndLaunched(consumer3, 3);

        Queue newConfig = JacksonUtil.clone(queue);
        newConfig.setConsumerPerPartition(false);
        consumerManager.update(newConfig);

        verifyUnsubscribedAndStopped(consumer1);
        verifyUnsubscribedAndStopped(consumer2);
        verifyUnsubscribedAndStopped(consumer3);
        verifySubscribedAndLaunched(getConsumer(), partitions);
    }

    @Test
    public void testStop() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        consumerManager.update(createTpis(1));
        TestConsumer consumer = getConsumer(1);
        verifySubscribedAndLaunched(consumer, 1);
        verify(queueFactory, times(1)).createToRuleEngineMsgConsumer(any(), any());
        verify(queueFactory, never()).createToRuleEngineMsgConsumer(any());

        consumerManager.stop();
        consumerManager.update(createTpis(1, 2, 3, 4)); // to check that no new tasks after stop are processed
        consumerManager.update(createTpis(5, 6, 7));

        verifyUnsubscribedAndStopped(consumer);
        verifyNoMoreInteractions(queueFactory);
    }

    @Test
    public void testDelete_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2);
        consumerManager.update(partitions);
        TestConsumer consumer1 = getConsumer(1);
        TestConsumer consumer2 = getConsumer(2);
        verifySubscribedAndLaunched(consumer1, 1);
        verifySubscribedAndLaunched(consumer2, 2);
        verifyMsgProcessed(consumer1.testMsg);
        verifyMsgProcessed(consumer2.testMsg);

        consumerManager.delete(true);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(ruleEngineMsgProducer).send(any(), any(), any());
                });
        clearInvocations(actorContext);
        verify(consumer1, never()).unsubscribe();
        verify(consumer2, never()).unsubscribe();
        int msgCount = totalConsumedMsgs.get();

        await().atLeast(2, TimeUnit.SECONDS) // based on topicDeletionDelayInSec(5) = 5 - ( 3 seconds the code may execute starting consumerManager.delete() call)
                .atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    partitions.stream()
                            .map(TopicPartitionInfo::getFullTopicName)
                            .forEach(topic -> {
                                verify(queueAdmin).deleteTopic(eq(topic));
                            });
                });
        verify(consumer1).unsubscribe();
        verify(consumer2).unsubscribe();

        int totalMovedMsgs = totalConsumedMsgs.get() - msgCount;
        assertThat(totalMovedMsgs).isNotZero();
        verify(ruleEngineMsgProducer, atLeast(totalMovedMsgs)).send(any(), any(), any());
        verify(actorContext, never()).tell(any());
        generateQueueMsgs = false;
    }

    @Test
    public void testDelete_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2);
        consumerManager.update(partitions);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);
        verifyMsgProcessed(consumer.testMsg);

        consumerManager.delete(true);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(ruleEngineMsgProducer).send(any(), any(), any());
                });
        clearInvocations(actorContext);
        verify(consumer, never()).unsubscribe();
        int msgCount = totalConsumedMsgs.get();

        await().atLeast(2, TimeUnit.SECONDS) // based on topicDeletionDelayInSec(5) = 5 - ( 3 seconds the code may execute starting consumerManager.delete() call)
                .atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    partitions.stream()
                            .map(TopicPartitionInfo::getFullTopicName)
                            .forEach(topic -> {
                                verify(queueAdmin).deleteTopic(eq(topic));
                            });
                });
        verify(consumer).unsubscribe();

        int movedMsgs = totalConsumedMsgs.get() - msgCount;
        assertThat(movedMsgs).isNotZero();
        verify(ruleEngineMsgProducer, atLeast(movedMsgs)).send(any(), any(), any());
        verify(actorContext, never()).tell(any());
        generateQueueMsgs = false;
    }

    @Test
    public void testManyDifferentUpdates() throws Exception {
        queue.setConsumerPerPartition(RandomUtils.nextBoolean());
        consumerManager.init(queue);

        Supplier<Queue> queueConfigUpdater = () -> {
            Queue oldConfig = consumerManager.getConfig();
            Queue newConfig = JacksonUtil.clone(oldConfig);
            newConfig.setConsumerPerPartition(RandomUtils.nextBoolean());
            newConfig.setPollInterval(RandomUtils.nextInt(100, 501));
            newConfig.setPartitions(RandomUtils.nextInt(1, 10));
            newConfig.setPackProcessingTimeout(RandomUtils.nextLong(100, 5001));
            newConfig.getSubmitStrategy().setType(SubmitStrategyType.values()[RandomUtils.nextInt(0, SubmitStrategyType.values().length)]);
            newConfig.getProcessingStrategy().setType(ProcessingStrategyType.values()[RandomUtils.nextInt(0, ProcessingStrategyType.values().length)]);
            log.info("Generated new config: consumerPerPartition={}, pollInterval={}, processingStrategy={}",
                    newConfig.isConsumerPerPartition(), newConfig.getPollInterval(), newConfig.getProcessingStrategy().getType());
            return newConfig;
        };
        Supplier<Set<TopicPartitionInfo>> partitionsUpdater = () -> {
            int partitionsCount = RandomUtils.nextInt(0, 20);
            int[] partitions = IntStream.generate(() -> RandomUtils.nextInt(0, 20))
                    .distinct().limit(partitionsCount)
                    .sorted().toArray();
            log.info("Generated new partitions: {}", Arrays.toString(partitions));
            return createTpis(partitions);
        };

        int iterations = 100;
        Queue latestConfig = queue;
        Set<TopicPartitionInfo> latestPartitions = Collections.emptySet();
        for (int i = 1; i <= iterations; i++) {
            boolean updateQueueConfig = RandomUtils.nextBoolean();
            boolean updatePartitions = !updateQueueConfig;
            if (updateQueueConfig) {
                latestConfig = queueConfigUpdater.get();
                consumerManager.update(latestConfig);
            }
            if (updatePartitions) {
                latestPartitions = partitionsUpdater.get();
                consumerManager.update(latestPartitions);
            }
            Thread.sleep(RandomUtils.nextLong(0, 200));
        }
        if (latestPartitions.isEmpty()) {
            do {
                latestPartitions = partitionsUpdater.get();
            } while (latestPartitions.isEmpty());
            consumerManager.update(latestPartitions);
        }

        Queue expectedConfig = latestConfig;
        Set<TopicPartitionInfo> expectedPartitions = latestPartitions;
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(consumerManager.getConfig()).isEqualTo(expectedConfig);
                    assertThat(consumerManager.getPartitions()).isEqualTo(expectedPartitions);
                });

        if (expectedConfig.isConsumerPerPartition()) {
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                for (TopicPartitionInfo partition : expectedPartitions) {
                    if (consumers.stream().noneMatch(consumer -> consumer.subscribed &&
                            consumer.pollingStarted && Set.of(partition).equals(consumer.getPartitions()))) {
                        return false;
                    }
                }
                return consumers.size() == expectedPartitions.size();
            });
        } else {
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                return consumers.size() == 1 && consumers.stream()
                        .anyMatch(consumer -> consumer.subscribed && consumer.pollingStarted &&
                                expectedPartitions.equals(consumer.getPartitions()));
            });
        }
        Mockito.reset(ruleEngineConsumerContext.getSubmitStrategyFactory());
        Mockito.reset(ruleEngineConsumerContext.getProcessingStrategyFactory());
        consumers.forEach(Mockito::clearInvocations);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            for (TestConsumer consumer : consumers) {
                verify(consumer, atLeastOnce().description("consumer " + consumer.topics)).poll(expectedConfig.getPollInterval());
            }
            verify(ruleEngineConsumerContext.getSubmitStrategyFactory(), atLeastOnce()).newInstance(any(), eq(expectedConfig.getSubmitStrategy()));
            verify(ruleEngineConsumerContext.getProcessingStrategyFactory(), atLeastOnce()).newInstance(any(), eq(expectedConfig.getProcessingStrategy()));
        });
    }

    private void verifySubscribedAndLaunched(TestConsumer consumer, Set<TopicPartitionInfo> expectedPartitions) {
        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> consumer.subscribed && consumer.getPartitions().equals(expectedPartitions) && consumer.pollingStarted);
        verify(consumer, times(1)).subscribe(any());
        verify(consumer).subscribe(eq(expectedPartitions));
        verify(consumer).doSubscribe(argThat(topics -> topics.containsAll(expectedPartitions)));
        verify(consumer, atLeastOnce()).poll(eq((long) queue.getPollInterval()));
        verify(consumer, atLeastOnce()).doPoll(eq((long) queue.getPollInterval()));
        verify(consumer, never()).unsubscribe();
        Mockito.reset(consumer);
    }

    private void verifySubscribedAndLaunched(TestConsumer consumer, int... expectedPartitions) {
        verifySubscribedAndLaunched(consumer, createTpis(expectedPartitions));
    }

    private void verifyUnsubscribedAndStopped(TestConsumer consumer) {
        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> !consumer.subscribed && !consumer.topics.isEmpty());
        verify(consumer, never()).subscribe(any());
        verify(consumer, never()).doSubscribe(any());
        assertThat(consumers).doesNotContain(consumer);
        Mockito.reset(consumer);
    }

    private void verifyNotTouched(TestConsumer consumer) {
        verify(consumer, never()).subscribe(any());
        verify(consumer, never()).subscribe();
        verify(consumer, never()).doSubscribe(any());
        verify(consumer, never()).unsubscribe();
        verify(consumer, never()).doUnsubscribe();
    }

    private void verifyMsgProcessed(TbMsg tbMsg) {
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(actorContext, atLeastOnce()).tell(argThat(msg -> {
                return ((QueueToRuleEngineMsg) msg).getMsg().getId().equals(tbMsg.getId());
            }));
        });
    }

    // for consumer-per-partition
    private TestConsumer getConsumer(TopicPartitionInfo tpi) {
        return await().atMost(5, TimeUnit.SECONDS)
                .until(() -> consumers.stream()
                        .filter(consumer -> consumer.getPartitions() != null &&
                                consumer.getPartitions().size() == 1 &&
                                consumer.getPartitions().contains(tpi))
                        .findFirst().orElse(null), Objects::nonNull);
    }

    private TestConsumer getConsumer(int partition) {
        return await().atMost(5, TimeUnit.SECONDS)
                .until(() -> consumers.stream()
                        .filter(consumer -> consumer.getPartitions() != null &&
                                consumer.getPartitions().size() == 1 &&
                                consumer.getPartitions().stream()
                                        .anyMatch(tpi -> tpi.getPartition().get().equals(partition)))
                        .findFirst().orElse(null), Objects::nonNull);
    }

    // for single consumer
    private TestConsumer getConsumer() {
        return await().atMost(5, TimeUnit.SECONDS)
                .until(() -> consumers.size() == 1 ? consumers.iterator().next() : null, Objects::nonNull);
    }

    private Set<TopicPartitionInfo> createTpis(int... partitions) {
        return Arrays.stream(partitions)
                .mapToObj(n -> TopicPartitionInfo.builder()
                        .tenantId(queue.getTenantId())
                        .topic(queue.getTopic())
                        .partition(n)
                        .myPartition(true)
                        .build())
                .collect(Collectors.toSet());
    }


    class TestConsumer extends AbstractTbQueueConsumerTemplate<TbMsg, TbProtoQueueMsg<ToRuleEngineMsg>> {

        @Getter
        private List<String> topics;

        private boolean subscribed;
        private boolean pollingStarted;

        private TbMsg testMsg;

        public TestConsumer(String topic) {
            super(topic);
        }

        @SneakyThrows
        @Override
        protected List<TbMsg> doPoll(long durationInMillis) {
            log.debug("doPoll({} ms)", durationInMillis);
            if (!subscribed) {
                throw new IllegalStateException("Cannot poll because not subscribed");
            }
            pollingStarted = true;
            if (testMsg != null && RandomUtils.nextBoolean()) {
                Thread.sleep(100);
                return List.of(testMsg);
            }
            return Collections.emptyList();
        }

        @Override
        protected TbProtoQueueMsg<ToRuleEngineMsg> decode(TbMsg tbMsg) throws IOException {
            log.debug("decode()");
            UUID tenantId = UUID.randomUUID();
            return new TbProtoQueueMsg<>(UUID.randomUUID(), ToRuleEngineMsg.newBuilder()
                    .setTenantIdMSB(tenantId.getMostSignificantBits())
                    .setTenantIdLSB(tenantId.getLeastSignificantBits())
                    .addRelationTypes("Success")
                    .setTbMsgProto(TbMsg.toProto(tbMsg))
                    .build());
        }

        @Override
        protected void doSubscribe(Set<TopicPartitionInfo> partitions) {
            this.topics = partitions.stream()
                    .map(TopicPartitionInfo::getFullTopicName)
                    .collect(Collectors.toList());
            log.debug("doSubscribe({})", topics);
            subscribed = true;
        }

        @Override
        protected void doCommit() {
            if (!subscribed) {
                throw new IllegalStateException("Cannot commit because not subscribed");
            }
            log.debug("doCommit() totalConsumedMsgs = {}", totalConsumedMsgs.incrementAndGet());
        }

        @Override
        public void unsubscribe() {
            super.unsubscribe();
            consumers.remove(this);
        }

        @Override
        protected void doUnsubscribe() {
            log.debug("doUnsubscribe()");
            if (!subscribed) {
                throw new IllegalStateException("Already unsubscribed!");
            }
            subscribed = false;
        }

        @Override
        protected boolean isLongPollingSupported() {
            return false;
        }

        public Set<TopicPartitionInfo> getPartitions() {
            return partitions;
        }

        public void setUpTestMsg() {
            testMsg = TbMsg.newMsg()
                    .type(TbMsgType.POST_TELEMETRY_REQUEST)
                    .originator(new DeviceId(UUID.randomUUID()))
                    .copyMetaData(new TbMsgMetaData())
                    .data("{}")
                    .build();
        }
    }

}
