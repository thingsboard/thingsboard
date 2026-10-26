// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import java.util.List;

public record RuleEngineMonitoringFilters(List<RuleEngineMonitoringQueueOption> queues,
                                          List<RuleEngineMonitoringOption> ruleChains,
                                          List<RuleEngineMonitoringRuleNodeOption> ruleNodes,
                                          List<String> serviceIds) {
}
