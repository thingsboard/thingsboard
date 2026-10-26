// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.ruleenginemonitoring;

import java.util.UUID;

public record RuleNodeTsKvEntry(Long bucketTime, Long execCount, Long errorCount, Long totalDurationMs,
                                Long avgDurationMs, Long maxDurationMs, UUID tenantId, String serviceId,
                                UUID queueId, UUID ruleChainId, UUID ruleNodeId) {
}
