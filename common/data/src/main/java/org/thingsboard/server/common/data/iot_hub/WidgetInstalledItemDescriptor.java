// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import lombok.Data;
import org.thingsboard.server.common.data.id.WidgetTypeId;

@Data
public class WidgetInstalledItemDescriptor implements IotHubInstalledItemDescriptor {

    private WidgetTypeId widgetTypeId;

}
