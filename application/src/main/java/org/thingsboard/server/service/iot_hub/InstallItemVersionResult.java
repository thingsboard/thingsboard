// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemDescriptor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallItemVersionResult {

    private boolean success;
    private String errorMessage;
    private IotHubInstalledItemDescriptor descriptor;

    public static InstallItemVersionResult success(IotHubInstalledItemDescriptor descriptor) {
        return new InstallItemVersionResult(true, null, descriptor);
    }

    public static InstallItemVersionResult error(String errorMessage) {
        return new InstallItemVersionResult(false, errorMessage, null);
    }

}
