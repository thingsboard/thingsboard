// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class DeviceProfileId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public DeviceProfileId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static DeviceProfileId fromString(String deviceProfileId) {
        return new DeviceProfileId(UUID.fromString(deviceProfileId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "DEVICE_PROFILE", allowableValues = "DEVICE_PROFILE")
    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }
}
