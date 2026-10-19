// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;

import java.util.UUID;

@ToString(callSuper = true)
public class EntityProfileData extends BaseEntityData<EntityFields> {

    private final EntityType entityType;

    public EntityProfileData(UUID entityId, EntityType entityType) {
        super(entityId);
        this.entityType = entityType;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

}
