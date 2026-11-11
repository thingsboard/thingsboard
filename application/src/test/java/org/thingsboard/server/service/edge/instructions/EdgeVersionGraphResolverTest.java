// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.instructions;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EdgeUpgradeInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeVersionGraphResolverTest {

    @Test
    void keepsHighestOptionNotAbovePlatform() {
        Map<String, List<EdgeUpgradeInfo>> graph = Map.of(
                "3.6.0", List.of(
                        new EdgeUpgradeInfo(false, "3.6.4"),
                        new EdgeUpgradeInfo(false, "3.7.0"),
                        new EdgeUpgradeInfo(false, "3.9.0")));

        Map<String, EdgeUpgradeInfo> resolved = EdgeVersionGraphResolver.resolve(graph, "3.7.0PE");

        assertThat(resolved.get("3.6.0").getNextEdgeVersion()).isEqualTo("3.7.0");
    }

    @Test
    void keepsRequiresUpdateDbOfTheSelectedOption() {
        Map<String, List<EdgeUpgradeInfo>> graph = Map.of(
                "3.6.0", List.of(
                        new EdgeUpgradeInfo(false, "3.6.4"),
                        new EdgeUpgradeInfo(true, "3.7.0"),
                        new EdgeUpgradeInfo(false, "3.9.0")));

        Map<String, EdgeUpgradeInfo> resolved = EdgeVersionGraphResolver.resolve(graph, "3.7.0PE");

        assertThat(resolved.get("3.6.0").getNextEdgeVersion()).isEqualTo("3.7.0");
        assertThat(resolved.get("3.6.0").isRequiresUpdateDb()).isTrue();
    }

    @Test
    void becomesTerminalWhenEveryOptionIsAbovePlatform() {
        Map<String, List<EdgeUpgradeInfo>> graph = Map.of(
                "3.6.0", List.of(new EdgeUpgradeInfo(false, "3.9.0")));

        Map<String, EdgeUpgradeInfo> resolved = EdgeVersionGraphResolver.resolve(graph, "3.7.0");

        assertThat(resolved.get("3.6.0").getNextEdgeVersion()).isNull();
        assertThat(resolved.get("3.6.0").isRequiresUpdateDb()).isFalse();
    }

    @Test
    void skipsOptionsWithNullNextVersion() {
        Map<String, List<EdgeUpgradeInfo>> graph = Map.of(
                "3.6.0", Arrays.asList(
                        new EdgeUpgradeInfo(false, null),
                        new EdgeUpgradeInfo(false, "3.6.4")));

        Map<String, EdgeUpgradeInfo> resolved = EdgeVersionGraphResolver.resolve(graph, "3.7.0");

        assertThat(resolved.get("3.6.0").getNextEdgeVersion()).isEqualTo("3.6.4");
    }

    @Test
    void stripsPlatformSuffixBeforeComparing() {
        Map<String, List<EdgeUpgradeInfo>> graph = Map.of(
                "3.6.0", List.of(new EdgeUpgradeInfo(false, "3.7.0")));

        // "3.7.0PE-SNAPSHOT" must be treated as 3.7.0 so the 3.7.0 upgrade stays eligible
        Map<String, EdgeUpgradeInfo> resolved = EdgeVersionGraphResolver.resolve(graph, "3.7.0PE-SNAPSHOT");

        assertThat(resolved.get("3.6.0").getNextEdgeVersion()).isEqualTo("3.7.0");
    }
}
