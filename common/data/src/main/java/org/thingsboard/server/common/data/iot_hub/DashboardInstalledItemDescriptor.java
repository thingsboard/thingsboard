// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;

@Data
public class DashboardInstalledItemDescriptor implements IotHubInstalledItemDescriptor {

    private DashboardId dashboardId;

}
