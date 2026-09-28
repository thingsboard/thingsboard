// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class AgentUpgradeRequest {

    @Schema(description = "Target agent image reference — a digest-pinned reference in production, or a tag.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String imageRef;
}
