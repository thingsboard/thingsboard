// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query;

import lombok.Data;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.edqs.data.EntityData;

import java.util.UUID;

@Data
public class SortableEntityData {

    private final EntityData entityData;
    private DataPoint sortValue;

    public UUID getId(){
        return entityData.getId();
    }

    public EntityId getEntityId() {
        return EntityIdFactory.getByTypeAndUuid(entityData.getEntityType(), entityData.getId());
    }
}
