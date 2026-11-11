// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemDescriptor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallItemVersionResult {

    private boolean success;
    private String errorMessage;
    private IotHubInstalledItemDescriptor descriptor;
    /** The installed item that was saved; null when unknown. */
    private UUID installedItemId;

    public static InstallItemVersionResult success(IotHubInstalledItemDescriptor descriptor) {
        return new InstallItemVersionResult(true, null, descriptor, null);
    }

    public static InstallItemVersionResult success(IotHubInstalledItemDescriptor descriptor, UUID installedItemId) {
        return new InstallItemVersionResult(true, null, descriptor, installedItemId);
    }

    public static InstallItemVersionResult error(String errorMessage) {
        return new InstallItemVersionResult(false, errorMessage, null, null);
    }

}
