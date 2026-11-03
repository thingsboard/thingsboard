// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
public class DeviceData implements Serializable {

    private static final long serialVersionUID = -3771567735290681274L;

    @Schema(description = "Device configuration for device profile type. DEFAULT is only supported value for now")
    private DeviceConfiguration configuration;
    @Schema(description = "Device transport configuration used to connect the device")
    private DeviceTransportConfiguration transportConfiguration;

}
