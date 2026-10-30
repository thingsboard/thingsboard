// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.UUID;

@JsonDeserialize(using = AliasEntityIdDeserializer.class)
@JsonSerialize(using = AliasEntityIdSerializer.class)
@Schema
public interface AliasEntityId extends EntityId {

    // The schema follows AliasEntityIdSerializer/AliasEntityIdDeserializer rather
    // than the getters: entityType carries either an EntityType or an
    // AliasEntityType name, id is only required for an EntityType, and
    // aliasEntityType is never written.
    @Override
    @Schema(implementation = String.class, requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Entity type (EntityType) or alias resolved for the current user (AliasEntityType: " +
                    "CURRENT_CUSTOMER, CURRENT_TENANT, CURRENT_USER, CURRENT_USER_OWNER)",
            example = "DEVICE")
    EntityType getEntityType();

    @Override
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            description = "ID of the entity, time-based UUID v1. Required for an entity type; for " +
                    "CURRENT_CUSTOMER, the customer used when the current user does not belong to one",
            example = "784f394c-42b6-435a-983c-b7beff2784f9")
    UUID getId();

    @Schema(hidden = true)
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
