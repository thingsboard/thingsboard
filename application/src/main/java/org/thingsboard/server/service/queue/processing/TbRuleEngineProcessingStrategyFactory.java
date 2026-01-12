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
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TbRuleEngineProcessingStrategyFactory {

    public TbRuleEngineProcessingStrategy newInstance(String name, ProcessingStrategy processingStrategy) {
        switch (processingStrategy.getType()) {
            case SKIP_ALL_FAILURES:
                return new SkipStrategy(name, false);
            case SKIP_ALL_FAILURES_AND_TIMED_OUT:
                return new SkipStrategy(name, true);
            case RETRY_ALL:
                return new RetryStrategy(name, true, true, true, processingStrategy);
            case RETRY_FAILED:
                return new RetryStrategy(name, false, true, false, processingStrategy);
            case RETRY_TIMED_OUT:
                return new RetryStrategy(name, false, false, true, processingStrategy);
            case RETRY_FAILED_AND_TIMED_OUT:
                return new RetryStrategy(name, false, true, true, processingStrategy);
            default:
                throw new RuntimeException("TbRuleEngineProcessingStrategy with type " + processingStrategy.getType() + " is not supported!");
        }
    }

    private static class RetryStrategy implements TbRuleEngineProcessingStrategy {
        private final String queueName;
        private final boolean retrySuccessful;
        private final boolean retryFailed;
        private final boolean retryTimeout;
        private final int maxRetries;
        private final double maxAllowedFailurePercentage;
        private final long maxPauseBetweenRetries;

        private long pauseBetweenRetries;

        private int initialTotalCount;
        private int retryCount;

        public RetryStrategy(String queueName, boolean retrySuccessful, boolean retryFailed, boolean retryTimeout, ProcessingStrategy processingStrategy) {
            this.queueName = queueName;
            this.retrySuccessful = retrySuccessful;
            this.retryFailed = retryFailed;
            this.retryTimeout = retryTimeout;
            this.maxRetries = processingStrategy.getRetries();
            this.maxAllowedFailurePercentage = processingStrategy.getFailurePercentage();
            this.pauseBetweenRetries = processingStrategy.getPauseBetweenRetries();
            this.maxPauseBetweenRetries = processingStrategy.getMaxPauseBetweenRetries();
        }

        @Override
        public boolean isSkipTimeoutMsgs() {
            return true;
        }

        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            if (result.isSuccess()) {
                log.trace("[{}] The result of the msg pack processing is successful, going to proceed with processing of the following msgs", queueName);
                return new TbRuleEngineProcessingDecision(true, null);
            } else {
                if (retryCount == 0) {
                    initialTotalCount = result.getPendingMap().size() + result.getFailedMap().size() + result.getSuccessMap().size();
                }
                retryCount++;
                double failedCount = result.getFailedMap().size() + result.getPendingMap().size();
                if (maxRetries > 0 && retryCount > maxRetries) {
                    log.debug("[{}] Skip reprocess of the rule engine pack due to max retries", queueName);
                    return new TbRuleEngineProcessingDecision(true, null);
                } else if (maxAllowedFailurePercentage > 0 && (failedCount / initialTotalCount) > maxAllowedFailurePercentage) {
                    log.debug("[{}] Skip reprocess of the rule engine pack due to max allowed failure percentage", queueName);
                    return new TbRuleEngineProcessingDecision(true, null);
                } else {
                    log.debug("[{}] The result of msg pack processing is unsuccessful, checking unprocessed msgs and going to reprocess them", queueName);
                    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> toReprocess = new ConcurrentHashMap<>(initialTotalCount);
                    if (retryFailed) {
                        result.getFailedMap().forEach(toReprocess::put);
                    } else if (log.isDebugEnabled() && !result.getFailedMap().isEmpty()) {
                        log.debug("[{}] Skipped {} failed messages due to the processing strategy configuration", queueName, result.getFailedMap().size());
                    }
                    if (retryTimeout) {
                        result.getPendingMap().forEach(toReprocess::put);
                    } else if (log.isDebugEnabled() && !result.getPendingMap().isEmpty()) {
                        log.debug("[{}] Skipped {} timedOut messages due to the processing strategy configuration", queueName, result.getPendingMap().size());
                    }
                    if (retrySuccessful) {
                        result.getSuccessMap().forEach(toReprocess::put);
                    } else if (log.isTraceEnabled() && !result.getSuccessMap().isEmpty()) {
                        log.trace("[{}] Skipped {} successful messages due to the processing strategy configuration", queueName, result.getSuccessMap().size());
                    }
                    if (CollectionUtils.isEmpty(toReprocess)) {
                        if (log.isDebugEnabled()) {
                            log.debug("[{}] Stopping the reprocessing logic due to reprocessing map is empty", queueName);
                        }
                        return new TbRuleEngineProcessingDecision(true, null);
                    }
                    log.debug("[{}] Going to reprocess {} messages", queueName, toReprocess.size());
                    if (log.isTraceEnabled()) {
                        toReprocess.forEach((id, msg) -> log.trace("Going to reprocess [{}]: {}", id, ProtoUtils.fromTbMsgProto(result.getQueueName(), msg.getValue(), TbMsgCallback.EMPTY)));
                    }
                    if (pauseBetweenRetries > 0) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pauseBetweenRetries));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (maxPauseBetweenRetries > pauseBetweenRetries) {
                            pauseBetweenRetries = Math.min(maxPauseBetweenRetries, pauseBetweenRetries * 2);
                        }
                    }
                    return new TbRuleEngineProcessingDecision(false, toReprocess);
                }
            }
        }
    }

    private static class SkipStrategy implements TbRuleEngineProcessingStrategy {

        private final String queueName;
        private final boolean skipTimeoutMsgs;

        public SkipStrategy(String name, boolean skipTimeoutMsgs) {
            this.queueName = name;
            this.skipTimeoutMsgs = skipTimeoutMsgs;
        }

        @Override
        public boolean isSkipTimeoutMsgs() {
            return skipTimeoutMsgs;
        }

        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            if (!result.isSuccess()) {
                log.debug("[{}] Reprocessing skipped for {} failed and {} timeout messages", queueName, result.getFailedMap().size(), result.getPendingMap().size());
            }
            if (log.isTraceEnabled()) {
                result.getFailedMap().forEach((id, msg) -> log.trace("Failed messages [{}]: {}", id, ProtoUtils.fromTbMsgProto(result.getQueueName(), msg.getValue(), TbMsgCallback.EMPTY)));
            }
            if (log.isTraceEnabled()) {
                result.getPendingMap().forEach((id, msg) -> log.trace("Timeout messages [{}]: {}", id, ProtoUtils.fromTbMsgProto(result.getQueueName(), msg.getValue(), TbMsgCallback.EMPTY)));
            }
            return new TbRuleEngineProcessingDecision(true, null);
        }
    }
}
