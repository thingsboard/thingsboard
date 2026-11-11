// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.EntityInfosConverter;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.AGENT_INFO_VIEW_TABLE_NAME)
public class AgentInfoEntity extends AbstractAgentEntity<AgentInfo> {

    @Column(name = ModelConstants.AGENT_CUSTOMER_TITLE_PROPERTY)
    private String customerTitle;

    @Column(name = ModelConstants.AGENT_CUSTOMER_IS_PUBLIC_PROPERTY)
    private boolean customerIsPublic;

    @Column(name = ModelConstants.AGENT_PROFILE_NAME_VIEW_PROPERTY)
    private String agentProfileName;

    @Column(name = ModelConstants.OWNER_NAME_COLUMN)
    private String ownerName;

    @Convert(converter = EntityInfosConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonType.class)
    @Column(name = ModelConstants.GROUPS_COLUMN)
    private List<EntityInfo> groups;

    @Column(name = "active")
    private boolean active;

    @Column(name = "agent_version")
    private String agentVersion;

    public AgentInfoEntity() {
        super();
    }

    @Override
    public AgentInfo toData() {
        AgentInfo info = new AgentInfo(super.toAgent(), customerTitle, customerIsPublic, agentProfileName);
        info.setOwnerName(ownerName);
        info.setGroups(groups);
        info.setActive(active);
        info.setAgentVersion(agentVersion);
        return info;
    }
}
