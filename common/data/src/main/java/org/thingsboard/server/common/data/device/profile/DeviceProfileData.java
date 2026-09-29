// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Schema
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @Schema(hidden = true)
    private List<DeviceProfileAlarm> alarms;
    @Schema(description = "Default device inactivity timeout in milliseconds. " +
            "Applied to all devices of this profile that don't have a per-device 'inactivityTimeout' server attribute. " +
            "If null, the server-level default from configuration is used.", example = "600000")
    private Long inactivityTimeoutMs;

}
