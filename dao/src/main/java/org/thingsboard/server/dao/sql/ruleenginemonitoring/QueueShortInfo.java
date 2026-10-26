// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import java.util.UUID;

public record QueueShortInfo(UUID id, UUID tenantId, String name, int partitions, boolean consumerPerPartition) {}
