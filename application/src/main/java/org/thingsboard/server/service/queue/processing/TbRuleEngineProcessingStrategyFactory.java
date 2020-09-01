/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.settings.TbRuleEngineQueueAckStrategyConfiguration;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class TbRuleEngineProcessingStrategyFactory {

    public TbRuleEngineProcessingStrategy newInstance(String name, TbRuleEngineQueueAckStrategyConfiguration configuration) {
        switch (configuration.getType()) {
            case "SKIP_ALL_FAILURES":
                return new SkipStrategy(name);
            case "RETRY_ALL":
                return new RetryStrategy(name, true, true, true, configuration);
            case "RETRY_FAILED":
                return new RetryStrategy(name, false, true, false, configuration);
            case "RETRY_TIMED_OUT":
                return new RetryStrategy(name, false, false, true, configuration);
            case "RETRY_FAILED_AND_TIMED_OUT":
                return new RetryStrategy(name, false, true, true, configuration);
            default:
                throw new RuntimeException("TbRuleEngineProcessingStrategy with type " + configuration.getType() + " is not supported!");
        }
    }

    private static class RetryStrategy implements TbRuleEngineProcessingStrategy {
        private final String queueName;
        private final boolean retrySuccessful;
        private final boolean retryFailed;
        private final boolean retryTimeout;
        private final int maxRetries;
        private final double maxAllowedFailurePercentage;
        private final long pauseBetweenRetries;

        private final boolean expPauseBetweenRetries;

        private long maxExpPauseBetweenRetries;
        private int maxExpDegreeValue;
        private AtomicInteger expDegreeStep;

        private int initialTotalCount;
        private int retryCount;

        public RetryStrategy(String queueName, boolean retrySuccessful, boolean retryFailed, boolean retryTimeout, TbRuleEngineQueueAckStrategyConfiguration configuration) {
            this.queueName = queueName;
            this.retrySuccessful = retrySuccessful;
            this.retryFailed = retryFailed;
            this.retryTimeout = retryTimeout;
            this.maxRetries = configuration.getRetries();
            this.maxAllowedFailurePercentage = configuration.getFailurePercentage();
            this.pauseBetweenRetries = configuration.getPauseBetweenRetries();
            this.expPauseBetweenRetries = configuration.isExpPauseBetweenRetries();
            if (this.expPauseBetweenRetries) {
                this.expDegreeStep = new AtomicInteger(1);
                this.maxExpPauseBetweenRetries = configuration.getMaxExpPauseBetweenRetries();
                this.maxExpDegreeValue = new Double(Math.log(maxExpPauseBetweenRetries) / Math.log(pauseBetweenRetries)).intValue();
            }
        }

        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            if (result.isSuccess()) {
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
                    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> toReprocess = new ConcurrentHashMap<>(initialTotalCount);
                    if (retryFailed) {
                        result.getFailedMap().forEach(toReprocess::put);
                    }
                    if (retryTimeout) {
                        result.getPendingMap().forEach(toReprocess::put);
                    }
                    if (retrySuccessful) {
                        result.getSuccessMap().forEach(toReprocess::put);
                    }
                    log.debug("[{}] Going to reprocess {} messages", queueName, toReprocess.size());
                    if (log.isTraceEnabled()) {
                        toReprocess.forEach((id, msg) -> log.trace("Going to reprocess [{}]: {}", id, TbMsg.fromBytes(result.getQueueName(), msg.getValue().getTbMsg().toByteArray(), TbMsgCallback.EMPTY)));
                    }
                    if (pauseBetweenRetries > 0) {
                        if (expPauseBetweenRetries) {
                            long pause;
                            if (maxExpDegreeValue > expDegreeStep.get()) {
                                pause = new Double(Math.pow(pauseBetweenRetries, expDegreeStep.getAndIncrement())).longValue();
                            } else {
                                pause = maxExpPauseBetweenRetries;
                            }
                            try {
                                Thread.sleep(TimeUnit.SECONDS.toMillis(
                                        pause));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            try {
                                Thread.sleep(TimeUnit.SECONDS.toMillis(pauseBetweenRetries));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    return new TbRuleEngineProcessingDecision(false, toReprocess);
                }
            }
        }
    }

    private static class SkipStrategy implements TbRuleEngineProcessingStrategy {

        private final String queueName;

        public SkipStrategy(String name) {
            this.queueName = name;
        }

        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            if (!result.isSuccess()) {
                log.debug("[{}] Reprocessing skipped for {} failed and {} timeout messages", queueName, result.getFailedMap().size(), result.getPendingMap().size());
            }
            if (log.isTraceEnabled()) {
                result.getFailedMap().forEach((id, msg) -> log.trace("Failed messages [{}]: {}", id, TbMsg.fromBytes(result.getQueueName(), msg.getValue().getTbMsg().toByteArray(), TbMsgCallback.EMPTY)));
            }
            if (log.isTraceEnabled()) {
                result.getPendingMap().forEach((id, msg) -> log.trace("Timeout messages [{}]: {}", id, TbMsg.fromBytes(result.getQueueName(), msg.getValue().getTbMsg().toByteArray(), TbMsgCallback.EMPTY)));
            }
            return new TbRuleEngineProcessingDecision(true, null);
        }
    }
}
