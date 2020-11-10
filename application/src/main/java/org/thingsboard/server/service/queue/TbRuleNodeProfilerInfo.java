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
package org.thingsboard.server.service.queue;

import lombok.Getter;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TbRuleNodeProfilerInfo {
    @Getter
    private final UUID ruleNodeId;
    @Getter
    private final String label;
    private AtomicInteger executionCount = new AtomicInteger(0);
    private AtomicLong executionTime = new AtomicLong(0);
    private AtomicLong maxExecutionTime = new AtomicLong(0);

    public TbRuleNodeProfilerInfo(RuleNodeInfo ruleNodeInfo) {
        this.ruleNodeId = ruleNodeInfo.getRuleNodeId().getId();
        this.label = ruleNodeInfo.toString();
    }

    public TbRuleNodeProfilerInfo(UUID ruleNodeId) {
        this.ruleNodeId = ruleNodeId;
        this.label = "";
    }

    public void record(long processingTime) {
        executionCount.incrementAndGet();
        executionTime.addAndGet(processingTime);
        while (true) {
            long value = maxExecutionTime.get();
            if (value >= processingTime) {
                break;
            }
            if (maxExecutionTime.compareAndSet(value, processingTime)) {
                break;
            }
        }
    }

    int getExecutionCount() {
        return executionCount.get();
    }

    long getMaxExecutionTime() {
        return maxExecutionTime.get();
    }

    double getAvgExecutionTime() {
        double executionCnt = (double) executionCount.get();
        if (executionCnt > 0) {
            return executionTime.get() / executionCnt;
        } else {
            return 0.0;
        }
    }

}