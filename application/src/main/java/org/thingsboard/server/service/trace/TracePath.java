// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record TracePath(String pathId, UUID tenantId, String traceId, UUID rootQueueId, String messageType,
                        UUID rootRuleChainId, Set<VisitedRuleNode> visitedRuleNodes,
                        Map<String, TraceGroup.RuleNodePathMetric> ruleNodePathMetrics,
                        long totalInQueueTime, long totalRuleNodeProcessingTime,
                        Map<UUID, Set<UUID>> incomingConnections, Map<UUID, Set<UUID>> outgoingConnections) {
}
