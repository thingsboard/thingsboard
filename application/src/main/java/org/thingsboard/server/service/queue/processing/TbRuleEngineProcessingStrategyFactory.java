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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TbRuleEngineProcessingStrategyFactory {

    @Value("${queue.rule_engine.strategy.type}")
    private String strategyType;
    @Value("${queue.rule_engine.strategy.retries:3}")
    private int maxRetries;
    @Value("${queue.rule_engine.strategy.failure_percentage:0}")
    private double maxAllowedFailurePercentage;
    @Value("${queue.rule_engine.strategy.pause_between_retries:3}")
    private long pauseBetweenRetries;


    public TbRuleEngineProcessingStrategy newInstance() {
        switch (strategyType) {
            case "SKIP_ALL":
                return new SkipStrategy();
            case "RETRY_ALL":
                return new RetryStrategy(true, true, true, maxRetries, maxAllowedFailurePercentage, pauseBetweenRetries);
            case "RETRY_FAILED":
                return new RetryStrategy(false, true, false, maxRetries, maxAllowedFailurePercentage, pauseBetweenRetries);
            case "RETRY_TIMED_OUT":
                return new RetryStrategy(false, false, true, maxRetries, maxAllowedFailurePercentage, pauseBetweenRetries);
            case "RETRY_FAILED_AND_TIMED_OUT":
                return new RetryStrategy(false, true, true, maxRetries, maxAllowedFailurePercentage, pauseBetweenRetries);
            default:
                throw new RuntimeException("TbRuleEngineProcessingStrategy with type " + strategyType + " is not supported!");
        }
    }

    private static class RetryStrategy implements TbRuleEngineProcessingStrategy {
        private final boolean retrySuccessful;
        private final boolean retryFailed;
        private final boolean retryTimeout;
        private final int maxRetries;
        private final double maxAllowedFailurePercentage;
        private final long pauseBetweenRetries;

        private int initialTotalCount;
        private int retryCount;

        public RetryStrategy(boolean retrySuccessful, boolean retryFailed, boolean retryTimeout, int maxRetries, double maxAllowedFailurePercentage, long pauseBetweenRetries) {
            this.retrySuccessful = retrySuccessful;
            this.retryFailed = retryFailed;
            this.retryTimeout = retryTimeout;
            this.maxRetries = maxRetries;
            this.maxAllowedFailurePercentage = maxAllowedFailurePercentage;
            this.pauseBetweenRetries = pauseBetweenRetries;
        }

        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            if (result.isSuccess()) {
                return new TbRuleEngineProcessingDecision(true, null);
            } else {
                if (retryCount == 0) {
                    initialTotalCount = result.getPendingMap().size() + result.getFailureMap().size() + result.getSuccessMap().size();
                }
                retryCount++;
                double failedCount = result.getFailureMap().size() + result.getPendingMap().size();
                if (maxRetries > 0 && retryCount > maxRetries) {
                    log.info("Skip reprocess of the rule engine pack due to max retries");
                    return new TbRuleEngineProcessingDecision(true, null);
                } else if (maxAllowedFailurePercentage > 0 && (failedCount / initialTotalCount) > maxAllowedFailurePercentage) {
                    log.info("Skip reprocess of the rule engine pack due to max allowed failure percentage");
                    return new TbRuleEngineProcessingDecision(true, null);
                } else {
                    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> toReprocess = new ConcurrentHashMap<>(initialTotalCount);
                    if (retryFailed) {
                        result.getFailureMap().forEach(toReprocess::put);
                    }
                    if (retryTimeout) {
                        result.getPendingMap().forEach(toReprocess::put);
                    }
                    if (retrySuccessful) {
                        result.getSuccessMap().forEach(toReprocess::put);
                    }
                    log.info("Going to reprocess {} messages", toReprocess.size());
                    //TODO: 2.5 Log most popular rule nodes by error count;
                    if (log.isTraceEnabled()) {
                        toReprocess.forEach((id, msg) -> log.trace("Going to reprocess [{}]: {}", id, msg.getValue()));
                    }
                    if (pauseBetweenRetries > 0) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pauseBetweenRetries));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return new TbRuleEngineProcessingDecision(false, toReprocess);
                }
            }
        }
    }

    private static class SkipStrategy implements TbRuleEngineProcessingStrategy {

        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            log.info("Skip reprocess of the rule engine pack");
            return new TbRuleEngineProcessingDecision(true, null);
        }
    }
}
