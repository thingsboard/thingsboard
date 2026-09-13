// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.relation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;

/**
 * Created by ashvayka on 02.05.17.
 */
@Data
@Schema
public class RelationEntityTypeFilter {

    public RelationEntityTypeFilter() {}

    public RelationEntityTypeFilter(String relationType, List<EntityType> entityTypes) {
        this(relationType, entityTypes, false);
    }

    public RelationEntityTypeFilter(String relationType, List<EntityType> entityTypes, boolean negate) {
        this.relationType = relationType;
        this.entityTypes = entityTypes;
        this.negate = negate;
    }

    @Schema(description = "Type of the relation between root entity and other entity (e.g. 'Contains' or 'Manages').", example = "Contains")
    private String relationType;

    @Schema(description = "Array of entity types to filter the related entities (e.g. 'DEVICE', 'ASSET').")
    private List<EntityType> entityTypes;

    @Schema(description = "Negate relation type between root entity and other entity.")
    private boolean negate;
}
