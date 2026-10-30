// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.ruleenginemonitoring;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup.RuleNodePathMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TraceGroupTest {

    @Test
    void givenOverlappingNodes_whenMergeRuleNodePathMetrics_thenSumsAndKeepsTheFirstKnownRuleChain() {
        UUID chainA = UUID.randomUUID();
        UUID chainB = UUID.randomUUID();
        Map<String, RuleNodePathMetric> target = new HashMap<>();
        target.put("node-1", new RuleNodePathMetric(10, 2, 1, null));

        TraceGroup.mergeRuleNodePathMetrics(target, Map.of(
                "node-1", new RuleNodePathMetric(5, 1, 0, chainA),
                "node-2", new RuleNodePathMetric(7, 3, 2, chainB)));
        TraceGroup.mergeRuleNodePathMetrics(target, Map.of("node-1", new RuleNodePathMetric(1, 1, 1, chainB)));
        TraceGroup.mergeRuleNodePathMetrics(target, null);

        assertThat(target.get("node-1")).isEqualTo(new RuleNodePathMetric(16, 4, 2, chainA));
        assertThat(target.get("node-2")).isEqualTo(new RuleNodePathMetric(7, 3, 2, chainB));
    }

}
