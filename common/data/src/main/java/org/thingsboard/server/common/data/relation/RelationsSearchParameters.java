// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

/**
 * Created by ashvayka on 03.05.17.
 */
@Schema
@Data
@AllArgsConstructor
public class RelationsSearchParameters {

    @Schema(description = "Root entity id to start search from.", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private UUID rootId;
    @Schema(description = "Type of the root entity.")
    private EntityType rootType;
    @Schema(description = "Type of the root entity.")
    private EntitySearchDirection direction;
    @Schema(description = "Type of the relation.")
    private RelationTypeGroup relationTypeGroup;
    @Schema(description = "Maximum level of the search depth.")
    private int maxLevel = 1;
    @Schema(description = "Fetch entities that match the last level of search. Useful to find Devices that are strictly 'maxLevel' relations away from the root entity.")
    private boolean fetchLastLevelOnly;

    public RelationsSearchParameters(EntityId entityId, EntitySearchDirection direction, int maxLevel, boolean fetchLastLevelOnly) {
        this(entityId, direction, maxLevel, RelationTypeGroup.COMMON, fetchLastLevelOnly);
    }

    public RelationsSearchParameters(EntityId entityId, EntitySearchDirection direction, int maxLevel, RelationTypeGroup relationTypeGroup, boolean fetchLastLevelOnly) {
        this.rootId = entityId.getId();
        this.rootType = entityId.getEntityType();
        this.direction = direction;
        this.maxLevel = maxLevel;
        this.relationTypeGroup = relationTypeGroup;
        this.fetchLastLevelOnly = fetchLastLevelOnly;
    }

    @JsonIgnore
    public EntityId getEntityId() {
        return EntityIdFactory.getByTypeAndUuid(rootType, rootId);
    }
}
