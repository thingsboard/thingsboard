// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

class AliasEntityIdImpl implements AliasEntityId {

    private UUID id;
    private EntityType entityType;
    private AliasEntityType aliasEntityType;
    private EntityId defaultEntityId;

    protected AliasEntityIdImpl(EntityId entityId) {
        this.id = entityId.getId();
        this.entityType = entityId.getEntityType();
    }

    protected AliasEntityIdImpl(AliasEntityType aliasEntityType, UUID id) {
        this.aliasEntityType = aliasEntityType;
        if (id != null) {
            switch (this.aliasEntityType) {
                case CURRENT_CUSTOMER:
                    this.defaultEntityId = new CustomerId(id);
                    break;
            }
        }
    }

    @Override
    public AliasEntityType getAliasEntityType() {
        return aliasEntityType;
    }

    @Override
    public EntityId defaultEntityId() {
        return defaultEntityId;
    }

    @Override
    public EntityId toEntityId() {
        return EntityIdFactory.getByTypeAndUuid(entityType, id);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof EntityId otherEntityId))
            return false;
        if (obj instanceof AliasEntityId otherAliasEntityId) {
            if (otherAliasEntityId.isAliasEntityId()) {
                if (!this.isAliasEntityId()) {
                    return false;
                }
                if (this.aliasEntityType != otherAliasEntityId.getAliasEntityType()) {
                    return false;
                }
                if (this.defaultEntityId != null && !this.defaultEntityId.equals(otherAliasEntityId.defaultEntityId())) {
                    return false;
                }
                if (this.defaultEntityId == null && otherAliasEntityId.defaultEntityId() != null) {
                    return false;
                }
            }
        }
        if (this.isAliasEntityId()) {
            return false;
        }
        if (id == null) {
            return otherEntityId.getId() == null;
        } else return id.equals(otherEntityId.getId());
    }
}
