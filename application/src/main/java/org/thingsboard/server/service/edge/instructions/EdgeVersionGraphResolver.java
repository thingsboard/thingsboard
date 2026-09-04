/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.instructions;

import org.thingsboard.common.util.TbVersionUtils;
import org.thingsboard.server.common.data.EdgeUpgradeInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EdgeVersionGraphResolver {

    private EdgeVersionGraphResolver() {
    }

    /**
     * Collapses a branching upgrade version graph into a single upgrade option per source version.
     * For each version the option with the highest {@code nextEdgeVersion} that is lower than or equal to the
     * given platform version is kept; if every option points to a newer version the source version becomes
     * terminal ({@code nextEdgeVersion == null}).
     */
    public static Map<String, EdgeUpgradeInfo> resolve(Map<String, List<EdgeUpgradeInfo>> versionGraph, String platformVersion) {
        String platform = TbVersionUtils.extractStartingDigits(platformVersion);
        Map<String, EdgeUpgradeInfo> resolved = new HashMap<>();
        for (var entry : versionGraph.entrySet()) {
            EdgeUpgradeInfo best = null;
            for (EdgeUpgradeInfo option : entry.getValue()) {
                String next = option.getNextEdgeVersion();
                // eligible only if next is present and lower than or equal to the platform version
                if (next == null || TbVersionUtils.compare(next, platform) > 0) {
                    continue;
                }
                // keep the option with the highest eligible nextEdgeVersion
                if (best == null || TbVersionUtils.compare(next, best.getNextEdgeVersion()) >= 0) {
                    best = option;
                }
            }
            resolved.put(entry.getKey(), best != null ? best : new EdgeUpgradeInfo(false, null));
        }
        return resolved;
    }
}
