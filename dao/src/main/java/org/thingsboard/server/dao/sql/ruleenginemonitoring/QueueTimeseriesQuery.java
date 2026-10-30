// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import java.util.Set;
import java.util.List;
import java.util.UUID;

public record QueueTimeseriesQuery(
        UUID tenantId,
        long startTs,
        long endTs,
        Set<RuleNodeDimension> groupBy,
        List<UUID> queueIds,
        List<UUID> ruleChainIds,
        List<UUID> ruleNodeIds,
        List<String> serviceIds,
        Long intervalMs
) {}
