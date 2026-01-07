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
