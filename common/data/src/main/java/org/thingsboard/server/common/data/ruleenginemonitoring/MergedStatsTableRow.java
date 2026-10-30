// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ruleenginemonitoring;

import java.util.UUID;

public record MergedStatsTableRow(
        UUID queueId,
        UUID ruleChainId,
        UUID ruleNodeId,
        String serviceId,

        Long execCount,
        Long errorCount,
        Long totalDurationMs,
        Long avgDurationMs,
        Long maxDurationMs,

        Long timeoutCount
) {}
