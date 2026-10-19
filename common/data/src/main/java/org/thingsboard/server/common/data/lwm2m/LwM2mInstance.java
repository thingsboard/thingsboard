// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class LwM2mInstance {
    @Schema(description = "LwM2M Instance id.", example = "0")
    int id;
    @Schema(description = "LwM2M Resource observe.")
    LwM2mResourceObserve[] resources;

}
