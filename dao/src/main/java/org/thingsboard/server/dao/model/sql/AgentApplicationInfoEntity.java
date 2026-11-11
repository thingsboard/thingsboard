// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentApplicationInfoEntity extends AgentApplicationEntity {

    private Long profileVersion;
    private String profileName;
    private String profileTemplateVersion;
    private String agentName;
    private UUID relatedEntityId;
    private String relatedEntityType;

    public AgentApplicationInfoEntity() {
        super();
    }

    public AgentApplicationInfoEntity(AgentApplicationEntity entity,
                                      Long profileVersion,
                                      String profileName,
                                      String profileTemplateVersion,
                                      UUID relatedEntityId,
                                      String relatedEntityType) {
        this(entity, profileVersion, profileName, profileTemplateVersion, null, relatedEntityId, relatedEntityType);
    }

    public AgentApplicationInfoEntity(AgentApplicationEntity entity,
                                      Long profileVersion,
                                      String profileName,
                                      String profileTemplateVersion,
                                      String agentName,
                                      UUID relatedEntityId,
                                      String relatedEntityType) {
        super(entity);
        this.profileVersion = profileVersion;
        this.profileName = profileName;
        this.profileTemplateVersion = profileTemplateVersion;
        this.agentName = agentName;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }

    @Override
    public AgentApplicationInfo toData() {
        // currentVersion is the app's own template version; nextVersion is enriched from the template registry
        // in the DAO layer (it depends on the external version graph, not the DB).
        AgentApplicationInfo info = new AgentApplicationInfo(super.toData(), getTemplateVersion(), null);
        info.setProfileConfigOutdated(profileVersion != null
                && !profileVersion.equals(info.getProfileConfigVersion()));
        info.setProfileName(profileName);
        info.setProfileTemplateVersion(profileTemplateVersion);
        info.setAgentName(agentName);
        if (relatedEntityId != null && relatedEntityType != null) {
            info.setRelatedEntityId(EntityIdFactory.getByTypeAndUuid(relatedEntityType, relatedEntityId));
        }
        return info;
    }

}
