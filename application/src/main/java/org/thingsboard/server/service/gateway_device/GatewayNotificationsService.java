// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.gateway_device;

import org.thingsboard.server.common.data.Device;

public interface GatewayNotificationsService {

    void onDeviceUpdated(Device device, Device oldDevice);

    void onDeviceDeleted(Device device);
}
