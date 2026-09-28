// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.Collections;

@Data
public abstract class TbAbstractTransformNodeConfigurationWithRelationQuery {

    private RelationsQuery relationsQuery;

    protected RelationsQuery getDefaultRelationQuery() {
        var relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        var entityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));
        return relationsQuery;
    }
    
}
