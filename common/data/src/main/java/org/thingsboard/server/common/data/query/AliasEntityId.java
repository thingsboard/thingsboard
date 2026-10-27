// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

@JsonDeserialize(using = AliasEntityIdDeserializer.class)
@JsonSerialize(using = AliasEntityIdSerializer.class)
@Schema
public interface AliasEntityId extends EntityId {

    AliasEntityType getAliasEntityType();

    EntityId defaultEntityId();

    EntityId toEntityId();

    @JsonIgnore
    default boolean isAliasEntityId() {
        return getAliasEntityType() != null;
    }

    static AliasEntityId fromEntityId(EntityId entityId) {
        if (entityId != null) {
            return new AliasEntityIdImpl(entityId);
        } else {
            return null;
        }
    }

    static AliasEntityId resolveAliasEntityId(AliasEntityId aliasEntityId, TenantId tenantId, UserId userId, EntityId userOwnerId) {
        if (aliasEntityId != null) {
            if (aliasEntityId.isAliasEntityId()) {
                AliasEntityType aliasEntityType = aliasEntityId.getAliasEntityType();
                switch (aliasEntityType) {
                    case CURRENT_CUSTOMER -> {
                        if (EntityType.CUSTOMER.equals(userOwnerId.getEntityType())) {
                            return fromEntityId(userOwnerId);
                        } else {
                            return fromEntityId(aliasEntityId.defaultEntityId());
                        }
                    }
                    case CURRENT_TENANT -> {
                        return fromEntityId(tenantId);
                    }
                    case CURRENT_USER -> {
                        return fromEntityId(userId);
                    }
                    case CURRENT_USER_OWNER -> {
                        return fromEntityId(userOwnerId);
                    }
                }
            } else {
                return aliasEntityId;
            }
        }
        return null;
    }

}
