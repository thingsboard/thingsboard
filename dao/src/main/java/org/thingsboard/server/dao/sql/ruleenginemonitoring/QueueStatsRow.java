// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import java.util.UUID;

public record QueueStatsRow(
        long bucketTime,
        UUID queueTenantId,
        UUID tenantId,
        UUID queueId,
        UUID ruleChainId,
        UUID lastVisitedRuleNodeId,
        String serviceId,
        long timeoutCount,
        long failureCount,
        long successCount
) {}
