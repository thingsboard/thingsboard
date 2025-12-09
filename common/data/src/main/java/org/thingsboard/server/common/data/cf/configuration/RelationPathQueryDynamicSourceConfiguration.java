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

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.List;

@Data
public class RelationPathQueryDynamicSourceConfiguration implements CfArgumentDynamicSourceConfiguration {

    private List<RelationPathLevel> levels;

    @Override
    public CFArgumentDynamicSourceType getType() {
        return CFArgumentDynamicSourceType.RELATION_PATH_QUERY;
    }

    @Override
    public void validate() {
        if (CollectionsUtil.isEmpty(levels)) {
            throw new IllegalArgumentException("At least one relation level must be specified!");
        }
        levels.forEach(RelationPathLevel::validate);
    }

    public List<EntityId> resolveEntityIds(List<EntityRelation> relations) {
        EntitySearchDirection lastLevelDirection = getLastLevel().direction();
        return switch (lastLevelDirection) {
            case FROM -> relations.stream().map(EntityRelation::getTo).toList();
            case TO -> relations.stream().map(EntityRelation::getFrom).toList();
        };
    }

    public void validateMaxRelationLevel(String argumentName, int maxAllowedRelationLevel) {
        if (levels.size() > maxAllowedRelationLevel) {
            throw new IllegalArgumentException("Max relation level is greater than configured " +
                                               "maximum allowed relation level in tenant profile: " + maxAllowedRelationLevel + " for argument: " + argumentName);
        }
    }

    public EntityRelationPathQuery toRelationPathQuery(EntityId entityId) {
        return new EntityRelationPathQuery(entityId, levels);
    }

    private RelationPathLevel getLastLevel() {
        return levels.get(levels.size() - 1);
    }

}
