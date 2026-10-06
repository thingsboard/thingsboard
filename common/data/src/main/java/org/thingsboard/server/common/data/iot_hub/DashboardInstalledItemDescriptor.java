// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;

/**
 * The IoT Hub no longer has Dashboard items. Kept only so installed-item rows created before that
 * can still be read and deleted.
 */
@Data
public class DashboardInstalledItemDescriptor implements IotHubInstalledItemDescriptor {

    private DashboardId dashboardId;

}
