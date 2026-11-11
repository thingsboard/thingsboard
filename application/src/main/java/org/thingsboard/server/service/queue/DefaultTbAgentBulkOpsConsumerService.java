// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.queue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.bulk.AgentBulkActionProcessingService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultTbAgentBulkOpsConsumerService {

    private static final String CONSUMER_NAME = "tb-agent-bulk-ops";

    @Value("${queue.agent.bulk-ops-poll-interval:1000}")
    private int pollInterval;
    @Value("${queue.agent.bulk-ops-processing-threads:2}")
    private int processingThreads;
    @Value("${queue.agent.bulk-ops-processing-queue-size:20}")
    private int processingQueueSize;
    @Value("${queue.agent.bulk-ops-pack-processing-timeout-ms:240000}")
    private long packProcessingTimeoutMs;

    private final TbCoreQueueFactory queueFactory;
    private final AgentBulkActionProcessingService bulkOperationService;

    private QueueConsumerManager<TbProtoQueueMsg<AgentBulkOperationMsg>> consumer;
    private ExecutorService consumerExecutor;
    private ExecutorService processingExecutor;

    @PostConstruct
    public void init() {
        consumerExecutor = ThingsBoardExecutors.newWorkStealingPool(1, CONSUMER_NAME);
        processingExecutor = ThingsBoardExecutors.newLimitedTasksExecutor(
                processingThreads, processingQueueSize, CONSUMER_NAME + "-processing");
        consumer = QueueConsumerManager.<TbProtoQueueMsg<AgentBulkOperationMsg>>builder()
                .name(CONSUMER_NAME)
                .msgPackProcessor(this::processMessages)
                .pollInterval(pollInterval)
                .consumerCreator(queueFactory::createAgentBulkOpsMsgConsumer)
                .consumerExecutor(consumerExecutor)
                .threadPrefix(CONSUMER_NAME)
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        consumer.subscribe();
        consumer.launch();
    }

    private void processMessages(List<TbProtoQueueMsg<AgentBulkOperationMsg>> msgs,
                                 TbQueueConsumer<TbProtoQueueMsg<AgentBulkOperationMsg>> consumer) {
        List<Future<?>> futures = new ArrayList<>(msgs.size());
        for (var msg : msgs) {
            futures.add(processingExecutor.submit(() -> {
                try {
                    bulkOperationService.processBulkOperation(msg.getValue());
                } catch (Exception e) {
                    log.error("Failed to process agent bulk operation message", e);
                }
            }));
        }
        awaitPack(futures);
        consumer.commit();
    }

    private void awaitPack(List<Future<?>> futures) {
        long deadline = System.currentTimeMillis() + packProcessingTimeoutMs;
        for (Future<?> future : futures) {
            long remaining = deadline - System.currentTimeMillis();
            try {
                future.get(Math.max(remaining, 0), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("Agent bulk operation pack did not complete within {} ms, committing anyway", packProcessingTimeoutMs);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                log.error("Agent bulk operation task failed", e.getCause());
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.stop();
        }
        if (processingExecutor != null) {
            processingExecutor.shutdown();
            try {
                if (!processingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Processing executor did not terminate in time, forcing shutdown");
                    processingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                processingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }
}
