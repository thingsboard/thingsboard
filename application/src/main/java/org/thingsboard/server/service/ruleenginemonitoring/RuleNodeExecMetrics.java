/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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
