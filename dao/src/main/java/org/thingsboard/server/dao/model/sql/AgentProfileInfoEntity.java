// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentProfileInfoEntity extends AgentProfileEntity {

    public AgentProfileInfoEntity() {
        super();
    }

    public AgentProfileInfoEntity(AgentProfileEntity profileEntity) {
        super(profileEntity);
    }

    @Override
    public AgentProfileInfo toData() {
        return new AgentProfileInfo(super.toData());
    }
}
