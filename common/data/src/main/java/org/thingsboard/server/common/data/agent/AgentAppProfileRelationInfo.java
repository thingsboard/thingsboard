// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.AgentProfileId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAppProfileRelationInfo extends AgentAppProfileInfo {

    @Schema(description = "JSON object with the Agent Profile Id this app profile is assigned to.", accessMode = Schema.AccessMode.READ_ONLY)
    private AgentProfileId agentProfileId;

    @Schema(description = "Number of AgentApplications using this app profile within the agent profile.", accessMode = Schema.AccessMode.READ_ONLY)
    private Long assignedApplicationsCount;

    @Schema(description = "Additional info of the assignment relation between the agent profile and this app profile.", accessMode = Schema.AccessMode.READ_ONLY)
    private JsonNode additionalInfo;

    public AgentAppProfileRelationInfo() {
        super();
    }

    public AgentAppProfileRelationInfo(AgentAppProfileInfo info, AgentProfileId agentProfileId, Long assignedApplicationsCount, JsonNode additionalInfo) {
        super(info, info.getTemplateCurrentVersion());
        this.agentProfileId = agentProfileId;
        this.assignedApplicationsCount = assignedApplicationsCount;
        this.additionalInfo = additionalInfo;
    }

}
