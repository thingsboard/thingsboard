// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Schema
@Data
public class DeviceProfileData implements Serializable {

    private static final long serialVersionUID = -3864805547939495272L;

    @Schema(description = "JSON object of device profile configuration")
    private DeviceProfileConfiguration configuration;
    @Valid
    @Schema(description = "JSON object of device profile transport configuration")
    private DeviceProfileTransportConfiguration transportConfiguration;
    @Schema(description = "JSON object of provisioning strategy type per device profile")
    private DeviceProfileProvisionConfiguration provisionConfiguration;
    @Valid
    @Schema(description = "JSON array of alarm rules configuration per device profile")
    private List<DeviceProfileAlarm> alarms;

}
