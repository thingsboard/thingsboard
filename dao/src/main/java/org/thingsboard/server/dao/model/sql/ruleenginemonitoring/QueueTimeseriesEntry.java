// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.ruleenginemonitoring;

import java.util.UUID;

public record QueueTimeseriesEntry(Long bucketTime, Long timeoutCount, Long failureCount, Long successCount,
                                   UUID tenantId, String serviceId, UUID queueId, UUID ruleChainId, UUID ruleNodeId,
                                   UUID queueTenantId) {
}
