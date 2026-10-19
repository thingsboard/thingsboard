// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class EntityGroupInfo extends EntityGroup {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of the entity group owners.")
    @ArraySchema(schema = @Schema(implementation = EntityId.class))
    private Set<EntityId> ownerIds;

    public EntityGroupInfo() {
        super();
    }

    public EntityGroupInfo(EntityGroupId id) {
        super(id);
    }

    public EntityGroupInfo(EntityGroup entityGroup) {
        super(entityGroup);
    }

    public EntityGroupInfo(EntityGroup entityGroup, Set<EntityId> ownerIds) {
        super(entityGroup);
        this.ownerIds = ownerIds;
    }
}
