// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class AgentUpgradeInfo implements Serializable {

    private static final long serialVersionUID = -2258743917452216440L;

    @Schema(description = "The next agent image tag in the upgrade chain, or null when this is the newest one.")
    private String nextAgentVersion;
}
