// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @ArraySchema(schema = @Schema(implementation = RelationPathLevel.class))
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
