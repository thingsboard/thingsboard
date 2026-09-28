// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Response payload for the install-agent-application endpoint. Carries both the created application "
        + "and the INSTALL event so the caller can open a progress dialog without a second round-trip.")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentAppInstallResponse {

    @Schema(description = "The newly created agent application.")
    private AgentApplication application;

    @Schema(description = "The INSTALL event created alongside the application.")
    private AgentAppEvent event;
}
