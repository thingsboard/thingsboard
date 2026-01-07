/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
