package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UpgradeInfo {
    private boolean requiresUpdateDb;
    private String nextEdgeVersion;
}
