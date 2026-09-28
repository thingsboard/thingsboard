// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAppProfileInfo extends AgentAppProfile {

    @Schema(description = "Current version of the template this profile points to.", accessMode = Schema.AccessMode.READ_ONLY)
    private String templateCurrentVersion;

    public AgentAppProfileInfo() {
        super();
    }

    public AgentAppProfileInfo(AgentAppProfile profile, String templateCurrentVersion) {
        super(profile);
        this.templateCurrentVersion = templateCurrentVersion;
    }

}
