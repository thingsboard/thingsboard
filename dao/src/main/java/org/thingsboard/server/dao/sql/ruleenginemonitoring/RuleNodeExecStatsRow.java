// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import java.util.UUID;

public record RuleNodeExecStatsRow(
        UUID tenantId,
        UUID queueId,
        UUID ruleChainId,
        UUID ruleNodeId,
        String serviceId,
        long bucketTime,
        long execCount,
        long errorCount,
        long totalDurationMs,
        long maxDurationMs
) {
    public static RuleNodeExecStatsRow toRow(UUID tenantId, UUID queueId, UUID ruleChainId, UUID ruleNodeId,
                                             String serviceId, long bucketTime,
                                             long execCount, long errorCount, long totalDurationMs, long maxDurationMs) {
        return new RuleNodeExecStatsRow(tenantId, queueId, ruleChainId, ruleNodeId, serviceId,
                bucketTime, execCount, errorCount, totalDurationMs, maxDurationMs);
    }
}
