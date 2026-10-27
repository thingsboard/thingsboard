// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ruleenginemonitoring;

public record MetricDelta(
        Long baseValue,
        Long compareValue,
        Long deltaValue,
        Double deltaPercent
) {}
