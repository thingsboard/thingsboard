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

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.DefaultTbQueueMsgHeaders;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.memory.DefaultInMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContextFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineConsumerLoopTest {

    TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    DeviceId deviceId = new DeviceId(UUID.randomUUID());

    InMemoryStorage storage;

    @Mock
    ActorSystemContext actorContext;
    @Mock
    StatsFactory statsFactory;
    @Mock
    TbRuleEngineQueueFactory queueFactory;
    @Mock
    RuleEngineStatisticsService statisticsService;
    @Mock
    TbServiceInfoProvider serviceInfoProvider;
    @Mock
    PartitionService partitionService;
    @Mock
    TbQueueProducerProvider producerProvider;
    @Mock
    TbQueueAdmin queueAdmin;
    @Mock
    TbMsgPackProcessingContextFactory packProcessingContextFactory;
    @Mock
    TbMsgPackProcessingContext packCtx;

    Queue mainQueue;

    TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer;

    TbRuleEngineConsumerContext ruleEngineConsumerContext;
    TbRuleEngineQueueConsumerManager consumerManager;

    ExecutorService consumersExecutor;
    ScheduledExecutorService scheduler;
    ExecutorService mgmtExecutor;

    @BeforeEach
    void setup() throws InterruptedException {
        consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("tb-rule-engine-consumer"));
        scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("tb-rule-engine-consumer-scheduler");
        mgmtExecutor = ThingsBoardExecutors.newWorkStealingPool(1, "tb-rule-engine-mgmt");

        mainQueue = new Queue();
        mainQueue.setTenantId(TenantId.SYS_TENANT_ID);
        mainQueue.setName("Main");
        mainQueue.setTopic("tb_rule_engine.main");
        mainQueue.setPollInterval(25);
        mainQueue.setPartitions(1);
        mainQueue.setConsumerPerPartition(false);
        mainQueue.setPackProcessingTimeout(2000L);

        var submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.BURST);
        submitStrategy.setBatchSize(1000);
        mainQueue.setSubmitStrategy(submitStrategy);

        var processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.0);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(3);
        mainQueue.setProcessingStrategy(processingStrategy);

        storage = new DefaultInMemoryStorage();

        consumer = spy(new InMemoryTbQueueConsumer<>(storage, mainQueue.getTopic()));
        given(queueFactory.createToRuleEngineMsgConsumer(eq(mainQueue), isNull())).willReturn(consumer);

        ruleEngineConsumerContext = new TbRuleEngineConsumerContext(
                actorContext, statsFactory, new TbRuleEngineSubmitStrategyFactory(), new TbRuleEngineProcessingStrategyFactory(),
                queueFactory, statisticsService, serviceInfoProvider, partitionService, producerProvider, queueAdmin
        );
        ruleEngineConsumerContext.setPollDuration(25);
        ruleEngineConsumerContext.setPackProcessingTimeout(2000);
        ruleEngineConsumerContext.setStatsEnabled(false); // true by default
        ruleEngineConsumerContext.setPrometheusStatsEnabled(false);
        ruleEngineConsumerContext.setTopicDeletionDelayInSec(15);
        ruleEngineConsumerContext.setMgmtThreadPoolSize(12);

        // Tell the (mock) context factory to return (mock) message pack context
        given(packProcessingContextFactory.create(
                eq(mainQueue.getName()),
                any(TbRuleEngineSubmitStrategy.class),
                eq(false)
        )).willAnswer(invocation -> {
            TbRuleEngineSubmitStrategy realStrategy = invocation.getArgument(1);
            when(packCtx.getPendingMap()).thenAnswer(i -> realStrategy.getPendingMap());
            when(packCtx.getFailedMap()).thenReturn(new ConcurrentHashMap<>());
            return packCtx;
        });

        // Tell the (mock) context's await() to return 'false' (always timeout) immediately
        given(packCtx.await(anyLong(), any(TimeUnit.class))).willReturn(false);

        consumerManager = TbRuleEngineQueueConsumerManager.create()
                .ctx(ruleEngineConsumerContext)
                .queueKey(new QueueKey(ServiceType.TB_RULE_ENGINE, mainQueue))
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .packProcessingContextFactory(packProcessingContextFactory)
                .build();
    }

    @AfterEach
    void destroy() {
        MoreExecutors.shutdownAndAwaitTermination(scheduler, Duration.ofSeconds(30));
        MoreExecutors.shutdownAndAwaitTermination(mgmtExecutor, Duration.ofSeconds(30));
        MoreExecutors.shutdownAndAwaitTermination(consumersExecutor, Duration.ofSeconds(30));
    }

    @Test
    void consumerLoopTest_verifyOperationsOrder() throws InterruptedException {
        // Create partition
        var partition = TopicPartitionInfo.builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .topic(mainQueue.getTopic())
                .partition(0)
                .myPartition(true)
                .useInternalPartition(false)
                .build();

        // Put 10k messages to the queue
        for (int i = 0; i < 10_000; i++) {
            var tbMsg = TbMsg.newMsg()
                    .type(TbMsgType.POST_TELEMETRY_REQUEST)
                    .originator(deviceId)
                    .data("{\"temperature\":123}")
                    .metaData(TbMsgMetaData.EMPTY)
                    .build();

            var toRuleEngineMsg = TransportProtos.ToRuleEngineMsg.newBuilder()
                    .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                    .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                    .setTbMsgProto(TbMsg.toProto(tbMsg))
                    .addAllRelationTypes(Set.of("Success"))
                    .build();

            storage.put(partition.getFullTopicName(), new TbProtoQueueMsg<>(UUID.randomUUID(), toRuleEngineMsg, new DefaultTbQueueMsgHeaders()));
        }

        // Count how many polls were made
        var totalPolls = new AtomicInteger(0);
        var emptyPolls = new AtomicInteger(0);
        doAnswer(invocation -> {
            totalPolls.incrementAndGet();
            @SuppressWarnings("unchecked")
            var messages = (List<TbQueueMsg>) invocation.callRealMethod();
            if (messages.isEmpty()) {
                emptyPolls.incrementAndGet();
            }
            return messages;
        }).when(consumer).poll(mainQueue.getPollInterval());

        // Count how many commits were made
        var totalCommits = new AtomicInteger(0);
        doAnswer(invocation -> {
            totalCommits.incrementAndGet();
            return invocation.callRealMethod();
        }).when(consumer).commit();

        // Initialize consumer
        consumerManager.init(mainQueue);

        // Assign partition to the consumer
        consumerManager.update(Set.of(partition));

        // Give some time for the consumer to get all messages
        await().atMost(Duration.ofSeconds(10L)).until(() -> storage.getLagTotal() == 0);

        // Stop consumer
        consumerManager.stop();
        consumerManager.awaitStop();

        // Determine number of non-empty consumer iterations made, since polling does not stop immediately after consuming all messages and may do a few empty polls
        int nonEmptyPolls = totalPolls.get() - emptyPolls.get();

        // Verify that there is 10 polls and 10 matching commits
        // Each poll consumes 1k messages and queue has 10k total, so that means 10k total msgs / 1k msgs per poll = 10 polls
        assertThat(nonEmptyPolls).isEqualTo(10).isEqualTo(totalCommits.get());

        // Verify that poll-await-commit cycle happened in order with correct await timeout
        InOrder inOrder = inOrder(consumer, packCtx);
        for (int i = 0; i < nonEmptyPolls; i++) {
            inOrder.verify(consumer).poll(mainQueue.getPollInterval());
            inOrder.verify(packCtx).await(mainQueue.getPackProcessingTimeout(), TimeUnit.MILLISECONDS);
            inOrder.verify(consumer).commit();
        }
    }

}
