// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.id.AgentProfileId;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAppProfileRelationInfoEntity extends AgentAppProfileInfoEntity {

    private UUID agentProfileId;
    private Long assignedApplicationsCount;
    private JsonNode additionalInfo;

    public AgentAppProfileRelationInfoEntity() {
        super();
    }

    public AgentAppProfileRelationInfoEntity(AgentAppProfileEntity entity, String templateCurrentVersion, UUID agentProfileId,
                                             Long assignedApplicationsCount, JsonNode additionalInfo) {
        super(entity, templateCurrentVersion);
        this.agentProfileId = agentProfileId;
        this.assignedApplicationsCount = assignedApplicationsCount;
        this.additionalInfo = additionalInfo;
    }

    /**
     * Projection without {@code assignedApplicationsCount}, for queries whose callers never read it.
     */
    public AgentAppProfileRelationInfoEntity(AgentAppProfileEntity entity, String templateCurrentVersion, UUID agentProfileId,
                                             JsonNode additionalInfo) {
        this(entity, templateCurrentVersion, agentProfileId, null, additionalInfo);
    }

    @Override
    public AgentAppProfileRelationInfo toData() {
        return new AgentAppProfileRelationInfo(super.toData(),
                agentProfileId != null ? new AgentProfileId(agentProfileId) : null,
                assignedApplicationsCount, additionalInfo);
    }

}
