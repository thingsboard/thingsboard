// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.DeviceProfileType;

import java.io.Serializable;

@Schema(
        description = "Device configuration",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DEFAULT", schema = DefaultDeviceConfiguration.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDeviceConfiguration.class, name = "DEFAULT")})
public interface DeviceConfiguration extends Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Device profile type")
    DeviceProfileType getType();

}
