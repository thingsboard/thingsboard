// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ruleenginemonitoring;

import java.util.UUID;

public record RuleEngineMonitoringQueueOption(UUID id, String name, UUID tenantId) {
}
