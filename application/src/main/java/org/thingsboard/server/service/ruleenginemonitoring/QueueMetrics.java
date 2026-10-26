// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

/**
 * Per-key outcome counter that lives in the accumulator across flushes; same drain/retire contract as
 * {@link RuleNodeExecMetrics}.
 */
public class QueueMetrics {
    private long count;
    private int idleFlushes;
    private boolean retired;

    /**
     * @return false if the entry has retired; the caller must re-fetch the entry from the accumulator and retry
     */
    public synchronized boolean add(long delta) {
        if (retired) {
            return false;
        }
        count += delta;
        return true;
    }

    /**
     * Returns the count since the previous drain and resets it.
     */
    public synchronized long drain(int maxIdleFlushes) {
        long drained = count;
        count = 0;
        if (drained == 0) {
            if (++idleFlushes >= maxIdleFlushes) {
                retired = true;
            }
        } else {
            idleFlushes = 0;
        }
        return drained;
    }

    public synchronized boolean isRetired() {
        return retired;
    }
}
