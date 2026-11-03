// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfileType;

@Data
public class DefaultDeviceProfileConfiguration implements DeviceProfileConfiguration {

    @Override
    public DeviceProfileType getType() {
        return DeviceProfileType.DEFAULT;
    }

}
