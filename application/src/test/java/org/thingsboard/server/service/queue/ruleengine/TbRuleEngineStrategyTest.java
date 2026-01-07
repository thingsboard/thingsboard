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
package org.thingsboard.server.service.queue.ruleengine;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgProcessingCtx;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.queue.ProcessingStrategyType.RETRY_ALL;
import static org.thingsboard.server.common.data.queue.ProcessingStrategyType.RETRY_FAILED;
import static org.thingsboard.server.common.data.queue.ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT;
import static org.thingsboard.server.common.data.queue.ProcessingStrategyType.RETRY_TIMED_OUT;
import static org.thingsboard.server.common.data.queue.ProcessingStrategyType.SKIP_ALL_FAILURES;
import static org.thingsboard.server.common.data.queue.ProcessingStrategyType.SKIP_ALL_FAILURES_AND_TIMED_OUT;
import static org.thingsboard.server.common.data.queue.SubmitStrategyType.BATCH;
import static org.thingsboard.server.common.data.queue.SubmitStrategyType.BURST;
import static org.thingsboard.server.common.data.queue.SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR;

@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
public class TbRuleEngineStrategyTest {

    @Mock
    private ActorSystemContext actorContext;
    @Mock
    private TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer;
    private TbRuleEngineConsumerContext ruleEngineConsumerContext;

    private static UUID tenantId = UUID.randomUUID();
    private static DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @BeforeEach
    public void beforeEach() {
        ruleEngineConsumerContext = new TbRuleEngineConsumerContext(
                actorContext, mock(), new TbRuleEngineSubmitStrategyFactory(),
                new TbRuleEngineProcessingStrategyFactory(), mock(), mock(),
                mock(), mock(), mock(), mock()
        );
        when(consumer.isStopped()).thenReturn(false);
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
                //BURST
                Arguments.of(BURST, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(1), new ProcessingData(1))),

                Arguments.of(BURST, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(1), new ProcessingData(1))),

                Arguments.of(BURST, RETRY_ALL, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, RETRY_ALL, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(2), new ProcessingData(2))),
                Arguments.of(BURST, RETRY_ALL, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(2), new ProcessingData(2))),

                Arguments.of(BURST, RETRY_FAILED, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, RETRY_FAILED, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, RETRY_FAILED, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(1), new ProcessingData(1))),

                Arguments.of(BURST, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(1), new ProcessingData(1))),

                Arguments.of(BURST, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BURST, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(1), new ProcessingData(1))),

                //BATCH
                Arguments.of(BATCH, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BATCH, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(1), new ProcessingData(0))),
                Arguments.of(BATCH, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(1), new ProcessingData(0))),

                Arguments.of(BATCH, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BATCH, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(1), new ProcessingData(0))),
                Arguments.of(BATCH, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(1), new ProcessingData(0))),

                Arguments.of(BATCH, RETRY_ALL, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BATCH, RETRY_ALL, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(2), new ProcessingData(1))),
                Arguments.of(BATCH, RETRY_ALL, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(2), new ProcessingData(1))),

                Arguments.of(BATCH, RETRY_FAILED, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BATCH, RETRY_FAILED, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(1), new ProcessingData(0))),
                Arguments.of(BATCH, RETRY_FAILED, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(1), new ProcessingData(0))),

                Arguments.of(BATCH, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BATCH, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BATCH, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(1), new ProcessingData(1))),

                Arguments.of(BATCH, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BATCH, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(BATCH, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(1), new ProcessingData(1))),

                //SEQUENTIAL_BY_ORIGINATOR
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(0), new ProcessingData(0))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, SKIP_ALL_FAILURES, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(0), new ProcessingData(0))),

                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(0), new ProcessingData(0))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, SKIP_ALL_FAILURES_AND_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(0), new ProcessingData(0))),

                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_ALL, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_ALL, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_ALL, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(1), new ProcessingData(1))),

                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_FAILED, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_FAILED, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(0), new ProcessingData(0))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_FAILED, 3, List.of(new ProcessingData(false, true, 1), new ProcessingData(0), new ProcessingData(0))),

                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(1), new ProcessingData(1))),

                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(1), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(true, false, 2), new ProcessingData(1), new ProcessingData(1))),
                Arguments.of(SEQUENTIAL_BY_ORIGINATOR, RETRY_FAILED_AND_TIMED_OUT, 3, List.of(new ProcessingData(false, true, 2), new ProcessingData(1), new ProcessingData(1)))
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void processMsgsTest(SubmitStrategyType submitStrategyType, ProcessingStrategyType processingStrategyType, int retries, List<ProcessingData> processingData) throws Exception {
        var queue = new Queue();
        queue.setName("Test");
        queue.setPackProcessingTimeout(100);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(submitStrategyType);
        submitStrategy.setBatchSize(2);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(processingStrategyType);
        processingStrategy.setRetries(retries);
        queue.setProcessingStrategy(processingStrategy);

        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queue);
        var consumerManager = TbRuleEngineQueueConsumerManager.create()
                .ctx(ruleEngineConsumerContext)
                .queueKey(queueKey)
                .build();

        consumerManager.init(queue);

        Map<UUID, ProcessingData> msgsMap = processingData.stream().collect(Collectors.toMap(data -> data.tbMsg.getId(), data -> data));
        Map<UUID, AtomicInteger> attemptsMap = new HashMap<>();
        Map<UUID, AtomicBoolean> failedMap = new HashMap<>();
        Map<UUID, AtomicBoolean> timeoutMap = new HashMap<>();

        doAnswer(inv -> {
            QueueToRuleEngineMsg msg = inv.getArgument(0);

            UUID msgId = msg.getMsg().getId();
            var data = msgsMap.get(msgId);

            var attempts = attemptsMap.computeIfAbsent(msgId, key -> new AtomicInteger(0));
            Assertions.assertTrue(attempts.getAndIncrement() <= data.attempts);

            var callback = msg.getMsg().getCallback();

            if (data.shouldFailed) {
                var alreadyFailed = failedMap.computeIfAbsent(msgId, key -> new AtomicBoolean(false));
                if (!alreadyFailed.getAndSet(true)) {
                    callback.onFailure(new RuleEngineException("Failed to process test msg!"));
                    return null;
                }
            }

            if (data.shouldTimeout) {
                var alreadyTimedOuted = timeoutMap.computeIfAbsent(msgId, key -> new AtomicBoolean(false));
                if (!alreadyTimedOuted.getAndSet(true)) {
                    return null;
                }
            }

            callback.onSuccess();
            return null;
        }).when(actorContext).tell(any());

        List<TbProtoQueueMsg<ToRuleEngineMsg>> protoMsgs = processingData.stream()
                .map(data -> data.tbMsg)
                .map(this::toProto)
                .toList();

        consumerManager.processMsgs(protoMsgs, consumer, queueKey, queue);

        processingData.forEach(data -> {
            verify(actorContext, times(data.attempts)).tell(argThat(msg ->
                    ((QueueToRuleEngineMsg) msg).getMsg().getId().equals(data.tbMsg.getId())
            ));
        });
    }

    private static TbMsg createRandomMsg() {
        return TbMsg.newMsg()
                .id(UUID.randomUUID())
                .type("test type")
                .originator(deviceId)
                .dataType(TbMsgDataType.TEXT)
                .data("test data")
                .ctx(new TbMsgProcessingCtx())
                .build();
    }

    private TbProtoQueueMsg<ToRuleEngineMsg> toProto(TbMsg tbMsg) {
        return new TbProtoQueueMsg<>(UUID.randomUUID(), ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .addRelationTypes("Success")
                .setTbMsgProto(TbMsg.toProto(tbMsg))
                .build());
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString(exclude = "tbMsg")
    private static class ProcessingData {
        private final TbMsg tbMsg = createRandomMsg();
        private final boolean shouldFailed;
        private final boolean shouldTimeout;
        private final int attempts;

        public ProcessingData(int attempts) {
            shouldFailed = false;
            shouldTimeout = false;
            this.attempts = attempts;
        }
    }

}
