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
public class UpdateItemVersionResult {

    private boolean success;
    private boolean entityModified;
    private String errorMessage;
    private IotHubInstalledItemDescriptor descriptor;

    public static UpdateItemVersionResult success(IotHubInstalledItemDescriptor descriptor) {
        return new UpdateItemVersionResult(true, false, null, descriptor);
    }

    public static UpdateItemVersionResult entityModified() {
        return new UpdateItemVersionResult(false, true, null, null);
    }

    public static UpdateItemVersionResult error(String errorMessage) {
        return new UpdateItemVersionResult(false, false, errorMessage, null);
    }

}
