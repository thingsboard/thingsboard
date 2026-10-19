// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class LwM2mObject {
    @Schema(description = "LwM2M Object id.", example = "19")
    int id;
    @Schema(description = "LwM2M Object key id.", example = "19_1.0")
    String keyId;
    @Schema(description = "LwM2M Object name.", example = "BinaryAppDataContainer")
    String name;
    @Schema(description = "LwM2M Object multiple.", example = "true")
    boolean multiple;
    @Schema(description = "LwM2M Object mandatory.", example = "false")
    boolean mandatory;
    @Schema(description = "LwM2M Object instances.")
    LwM2mInstance [] instances;
}
