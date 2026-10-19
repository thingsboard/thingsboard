// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.security.DeviceCredentials;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class DeviceExportData extends EntityExportData<Device> {

    @Override
    public EntityType getEntityType() { return EntityType.DEVICE; }

    @JsonProperty(index = 3)
    @JsonIgnoreProperties({"id", "deviceId", "createdTime", "version"})
    private DeviceCredentials credentials;

    @JsonIgnore
    @Override
    public boolean hasCredentials() {
        return credentials != null;
    }

}
