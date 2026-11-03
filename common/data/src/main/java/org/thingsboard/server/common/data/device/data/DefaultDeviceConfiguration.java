// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfileType;

@Schema
@Data
public class DefaultDeviceConfiguration implements DeviceConfiguration {

    private static final long serialVersionUID = -2225378639573611325L;

    @Override
    public DeviceProfileType getType() {
        return DeviceProfileType.DEFAULT;
    }

}
