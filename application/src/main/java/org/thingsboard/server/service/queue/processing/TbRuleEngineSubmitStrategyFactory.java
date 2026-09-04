// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.queue.SubmitStrategy;

@Component
@Slf4j
public class TbRuleEngineSubmitStrategyFactory {

    public TbRuleEngineSubmitStrategy newInstance(String name, SubmitStrategy submitStrategy) {
        switch (submitStrategy.getType()) {
            case BURST:
                return new BurstTbRuleEngineSubmitStrategy(name);
            case BATCH:
                return new BatchTbRuleEngineSubmitStrategy(name, submitStrategy.getBatchSize());
            case SEQUENTIAL_BY_ORIGINATOR:
                return new SequentialByOriginatorIdTbRuleEngineSubmitStrategy(name);
            case SEQUENTIAL_BY_TENANT:
                return new SequentialByTenantIdTbRuleEngineSubmitStrategy(name);
            case SEQUENTIAL:
                return new SequentialTbRuleEngineSubmitStrategy(name);
            default:
                throw new RuntimeException("TbRuleEngineProcessingStrategy with type " + submitStrategy.getType() + " is not supported!");
        }
    }

}
