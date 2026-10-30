// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

/**
 * Per-key execution metrics that live in the accumulator across flushes. Recording and draining share the entry's
 * lock, so a flush never loses a concurrent update or reads a half-applied one. An entry left idle for
 * {@code maxIdleFlushes} drains retires: it accepts no more records and is removed from the accumulator.
 */
public class RuleNodeExecMetrics {
    private long execCount;
    private long errorCount;
    private long totalDurationMs;
    private long maxDurationMs;
    private int idleFlushes;
    private boolean retired;

    /**
     * @return false if the entry has retired; the caller must re-fetch the entry from the accumulator and retry
     */
    public synchronized boolean record(long durationMs, boolean failure) {
        if (retired) {
            return false;
        }
        execCount++;
        if (failure) {
            errorCount++;
        }
        totalDurationMs += durationMs;
        maxDurationMs = Math.max(maxDurationMs, durationMs);
        return true;
    }

    /**
     * Returns what was recorded since the previous drain and resets it, or null if nothing was.
     */
    public synchronized Snapshot drain(int maxIdleFlushes) {
        if (execCount == 0) {
            if (++idleFlushes >= maxIdleFlushes) {
                retired = true;
            }
            return null;
        }
        Snapshot snapshot = new Snapshot(execCount, errorCount, totalDurationMs, maxDurationMs);
        execCount = 0;
        errorCount = 0;
        totalDurationMs = 0;
        maxDurationMs = 0;
        idleFlushes = 0;
        return snapshot;
    }

    public synchronized boolean isRetired() {
        return retired;
    }

    public record Snapshot(long execCount, long errorCount, long totalDurationMs, long maxDurationMs) {
    }
}
