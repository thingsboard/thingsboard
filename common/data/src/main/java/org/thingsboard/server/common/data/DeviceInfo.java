// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeviceInfo extends Device {

    @Valid
    @Schema(description = "Owner name", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;

    @Valid
    @Schema(description = "Groups", accessMode = Schema.AccessMode.READ_ONLY)
    private List<EntityInfo> groups;

    @Schema(description = "Device active flag.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean active;

    public DeviceInfo() {
        super();
    }

    public DeviceInfo(Device device, String ownerName, List<EntityInfo> groups, boolean active) {
        super(device);
        this.ownerName = ownerName;
        this.groups = groups;
        this.active = active;
    }

}
