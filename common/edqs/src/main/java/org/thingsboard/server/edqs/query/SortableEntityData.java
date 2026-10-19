// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
    private boolean readAttrs;
    private boolean readTs;

    public UUID getId(){
        return entityData.getId();
    }

    public EntityId getEntityId() {
        return EntityIdFactory.getByTypeAndUuid(entityData.getEntityType(), entityData.getId());
    }
}
