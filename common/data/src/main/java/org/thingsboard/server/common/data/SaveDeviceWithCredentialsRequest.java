// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.security.DeviceCredentials;

;

@Schema
@Data
public class SaveDeviceWithCredentialsRequest {

    @Schema(description = "The JSON with device entity.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private final Device device;
    @Schema(description = "The JSON with credentials entity.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private final DeviceCredentials credentials;

}
