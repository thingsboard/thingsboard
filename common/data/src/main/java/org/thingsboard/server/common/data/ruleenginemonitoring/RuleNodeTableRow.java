// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ruleenginemonitoring;

import java.util.UUID;

public record RuleNodeTableRow(Long execCount, Long errorCount, Long totalDurationMs, Long avgDurationMs,
                                Long maxDurationMs, UUID tenantId, String serviceId, UUID queueId,
                                UUID ruleChainId, UUID ruleNodeId) {
}
