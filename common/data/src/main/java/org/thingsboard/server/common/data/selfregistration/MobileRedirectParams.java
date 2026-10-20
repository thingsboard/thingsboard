// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class MobileRedirectParams {

    @Schema(description = "Mobile application verification settings. Used for callback to mobile application once user is registered.")
    private String scheme;
    @Schema(description = "Mobile application verification settings. Used for callback to mobile application once user is registered.")
    private String host;

}
