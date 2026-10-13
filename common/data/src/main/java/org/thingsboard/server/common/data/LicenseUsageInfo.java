// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class LicenseUsageInfo extends LicenseInfo {

    private long devicesCount;
    private long assetsCount;
    private long edgesCount;
    private long dashboardsCount;
    private long integrationsCount;

    public LicenseUsageInfo() {}

    public LicenseUsageInfo(LicenseInfo licenseInfo) {
        super(licenseInfo);
    }
}
