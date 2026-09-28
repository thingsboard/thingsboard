// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

@Data
public class EntityGroupFilter implements EntityFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_GROUP;
    }

    private EntityType groupType;
    private String entityGroup;
    private boolean groupStateEntity;
    private EntityType defaultStateGroupType;
    private String defaultStateEntityGroup;

}
