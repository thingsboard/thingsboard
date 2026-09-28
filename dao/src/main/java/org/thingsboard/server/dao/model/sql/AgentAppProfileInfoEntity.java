// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAppProfileInfoEntity extends AgentAppProfileEntity {

    private String templateCurrentVersion;

    public AgentAppProfileInfoEntity() {
        super();
    }

    public AgentAppProfileInfoEntity(AgentAppProfileEntity entity, String templateCurrentVersion) {
        super(entity);
        this.templateCurrentVersion = templateCurrentVersion;
    }

    @Override
    public AgentAppProfileInfo toData() {
        return new AgentAppProfileInfo(super.toData(), templateCurrentVersion);
    }

}
