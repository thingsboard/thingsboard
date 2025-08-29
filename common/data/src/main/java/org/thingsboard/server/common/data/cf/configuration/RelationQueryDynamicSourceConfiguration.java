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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.Collections;
import java.util.List;

@Data
public class RelationQueryDynamicSourceConfiguration implements CfArgumentDynamicSourceConfiguration {

    private int maxLevel;
    private boolean fetchLastLevelOnly;
    private EntitySearchDirection direction;
    private String relationType;

    @Override
    public CFArgumentDynamicSourceType getType() {
        return CFArgumentDynamicSourceType.RELATION_QUERY;
    }

    @Override
    public void validate() {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Relation query dynamic source configuration max relation level can't be less than 1!");
        }
        if (maxLevel > 2) {
            throw new IllegalArgumentException("Relation query dynamic source configuration max relation level can't be greater than 2!");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Relation query dynamic source configuration direction must be specified!");
        }
        if (StringUtils.isBlank(relationType)) {
            throw new IllegalArgumentException("Relation query dynamic source configuration relation type must be specified!");
        }
    }

    @JsonIgnore
    public boolean isSimpleRelation() {
        return maxLevel == 1;
    }

    public EntityRelationsQuery toEntityRelationsQuery(EntityId rootEntityId) {
        if (isSimpleRelation()) {
            throw new IllegalArgumentException("Entity relations query can't be created for a simple relation!");
        }
        var entityRelationsQuery = new EntityRelationsQuery();
        entityRelationsQuery.setParameters(new RelationsSearchParameters(rootEntityId, direction, maxLevel, fetchLastLevelOnly));
        entityRelationsQuery.setFilters(Collections.singletonList(new RelationEntityTypeFilter(relationType, Collections.emptyList())));
        return entityRelationsQuery;
    }

    public List<EntityId> resolveEntityIds(List<EntityRelation> relations) {
        return switch (direction) {
            case FROM -> relations.stream().map(EntityRelation::getTo).toList();
            case TO -> relations.stream().map(EntityRelation::getFrom).toList();
        };
    }

}
