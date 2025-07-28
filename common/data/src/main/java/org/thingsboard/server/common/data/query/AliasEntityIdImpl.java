/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
