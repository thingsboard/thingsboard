// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class HomeDashboardParams {

    @Schema(description = "Home dashboard Id to assign for the new user.", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private String id;
    @Schema(description = "Indicates if hide toolbar should be hidden.")
    private boolean hideToolbar;

}
