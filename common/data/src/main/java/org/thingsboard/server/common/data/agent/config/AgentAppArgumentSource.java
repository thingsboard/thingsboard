// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;

@Getter
public enum AgentAppArgumentSource {

    AGENT,
    OWNER,
    RELATED_ENTITY,
    TENANT,
    DEVICE(EntityType.DEVICE),
    ASSET(EntityType.ASSET),
    CUSTOMER(EntityType.CUSTOMER),
    EDGE(EntityType.EDGE);

    private final EntityType searchEntityType;

    AgentAppArgumentSource() {
        this(null);
    }

    AgentAppArgumentSource(EntityType searchEntityType) {
        this.searchEntityType = searchEntityType;
    }

    public boolean isConcreteEntityRef() {
        return searchEntityType != null;
    }

}
