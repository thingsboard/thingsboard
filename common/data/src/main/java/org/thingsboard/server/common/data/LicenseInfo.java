// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Data;

@Data
public class LicenseInfo {
    private long maxDevices;
    private long maxAssets;
    private long maxEdges;
    private long maxAgents;
    private boolean whiteLabelingEnabled;
    private boolean development;
    private String plan;

    public LicenseInfo() {}

    public LicenseInfo(LicenseInfo licenseInfo) {
        this.maxDevices = licenseInfo.getMaxDevices();
        this.maxAssets = licenseInfo.getMaxAssets();
        this.maxEdges = licenseInfo.getMaxEdges();
        this.maxAgents = licenseInfo.getMaxAgents();
        this.whiteLabelingEnabled = licenseInfo.isWhiteLabelingEnabled();
        this.development = licenseInfo.isDevelopment();
        this.plan = licenseInfo.getPlan();
    }
}
