// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.ruleenginemonitoring;

import java.util.UUID;

public record QueueLagEntry(Long bucketTime, UUID queueTenantId, UUID queueId, Long lag) {
}
