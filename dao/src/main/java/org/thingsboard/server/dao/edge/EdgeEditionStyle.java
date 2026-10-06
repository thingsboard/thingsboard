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
        return TbVersionUtils.compare(edgeVersion, "4.4") > 0 ? CE : PE;
    }

    public String replacePlaceholders(String instructions) {
        return instructions
                .replace("${TB_EDGE_REPO}", dockerRepo)
                .replace("${TB_EDGE_PKG_SUFFIX}", packageSuffix);
    }
}
