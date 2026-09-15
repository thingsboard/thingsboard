// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfile;

import java.io.Serializable;

@Data
@Builder
public class ValidateDeviceCredentialsResponse implements DeviceProfileAware, Serializable {

    private final TransportDeviceInfo deviceInfo;
    private final DeviceProfile deviceProfile;
    private final String credentials;

    public boolean hasDeviceInfo() {
        return deviceInfo != null;
    }
}
