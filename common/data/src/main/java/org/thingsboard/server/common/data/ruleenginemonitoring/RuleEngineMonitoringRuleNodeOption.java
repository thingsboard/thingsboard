// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ruleenginemonitoring;

import java.util.UUID;

public record RuleEngineMonitoringRuleNodeOption(UUID id, String name, UUID ruleChainId, String ruleChainName) {
}
