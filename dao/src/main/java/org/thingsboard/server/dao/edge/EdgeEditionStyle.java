// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.edge;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.common.util.TbVersionUtils;

@Getter
@RequiredArgsConstructor
public enum EdgeEditionStyle {
    CE("tb-edge", "EDGE", ""),
    PE("tb-edge-pe", "EDGEPE", "pe");

    private final String dockerRepo;
    private final String versionSuffix;
    private final String packageSuffix;

    public static EdgeEditionStyle getEdgeEditionStyle(String edgeVersion) {
        return TbVersionUtils.compare(edgeVersion, "4.4.0.1") >= 0 ? CE : PE;
    }

    /**
     * The edge version with the tag suffix its release is published under, e.g. {@code 4.4.0EDGEPE}, {@code 4.4.0.1EDGE}.
     * Agent app templates are registered under these versions.
     */
    public static String withVersionSuffix(String edgeVersion) {
        return edgeVersion + getEdgeEditionStyle(edgeVersion).getVersionSuffix();
    }

    public String replacePlaceholders(String instructions) {
        return instructions
                .replace("${TB_EDGE_REPO}", dockerRepo)
                .replace("${TB_EDGE_PKG_SUFFIX}", packageSuffix);
    }
}
