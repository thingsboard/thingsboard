// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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