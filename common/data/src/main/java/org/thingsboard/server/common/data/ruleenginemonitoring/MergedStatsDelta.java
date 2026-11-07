// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ruleenginemonitoring;

import java.util.UUID;

public record MergedStatsDelta(
        UUID queueId,
        UUID ruleChainId,
        UUID ruleNodeId,
        String serviceId,
        MetricDelta execCount,
        MetricDelta errorCount,
        MetricDelta totalDurationMs,
        MetricDelta avgDurationMs,
        MetricDelta maxDurationMs,
        MetricDelta timeoutCount
) {}
