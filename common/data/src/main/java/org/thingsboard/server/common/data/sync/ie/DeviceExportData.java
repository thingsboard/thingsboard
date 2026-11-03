// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class DeviceExportData extends EntityExportData<Device> {

    @JsonProperty(index = 3)
    @JsonIgnoreProperties({"id", "deviceId", "createdTime", "version"})
    private DeviceCredentials credentials;

    @JsonIgnore
    @Override
    public boolean hasCredentials() {
        return credentials != null;
    }

}
