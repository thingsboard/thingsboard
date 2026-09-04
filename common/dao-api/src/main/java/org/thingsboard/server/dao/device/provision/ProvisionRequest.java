// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device.provision;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.device.credentials.ProvisionDeviceCredentialsData;
import org.thingsboard.server.common.data.device.profile.ProvisionDeviceProfileCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

@Data
@AllArgsConstructor
public class ProvisionRequest {
    private String deviceName;
    private DeviceCredentialsType credentialsType;
    private ProvisionDeviceCredentialsData credentialsData;
    private ProvisionDeviceProfileCredentials credentials;
    private Boolean gateway;
}
