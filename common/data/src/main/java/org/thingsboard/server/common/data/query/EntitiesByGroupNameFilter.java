// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

@Data
public class EntitiesByGroupNameFilter implements EntityFilter {
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITIES_BY_GROUP_NAME;
    }

    private EntityType groupType;
    private EntityId ownerId;
    private String entityGroupNameFilter;
    private boolean groupStateEntity;
    private String stateEntityParamName;
}
