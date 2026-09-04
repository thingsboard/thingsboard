// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.DeviceId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeviceInfo extends Device {

    private static final long serialVersionUID = -3004579925090663691L;

    @Schema(description = "Title of the Customer that owns the device.", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;
    @Schema(description = "Indicates special 'Public' Customer that is auto-generated to use the devices on public dashboards.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean customerIsPublic;
    @Schema(description = "Name of the corresponding Device Profile.", accessMode = Schema.AccessMode.READ_ONLY)
    private String deviceProfileName;
    @Schema(description = "Device active flag.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean active;

    public DeviceInfo() {
        super();
    }

    public DeviceInfo(DeviceId deviceId) {
        super(deviceId);
    }

    public DeviceInfo(Device device, String customerTitle, boolean customerIsPublic, String deviceProfileName, boolean active) {
        super(device);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
        this.deviceProfileName = deviceProfileName;
        this.active = active;
    }
}
