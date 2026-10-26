// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import java.util.UUID;

public record QueueKey(UUID queueTenantId, UUID tenantId, UUID queueId, UUID lastVisitedRuleNodeId, String serviceId, MetricType metricType) {
}
