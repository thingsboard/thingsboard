// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

public class EdgeTypeSearchQueryProcessor extends AbstractEntitySearchQueryProcessor<EdgeSearchQueryFilter> {

    public EdgeTypeSearchQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EdgeSearchQueryFilter) query.getEntityFilter());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

    @Override
    protected boolean check(RelationInfo relationInfo) {
        EntityData<?> ed = relationInfo.getTarget();
        return super.check(relationInfo) &&
                (filter.getEdgeTypes() == null || filter.getEdgeTypes().contains(ed.getFields().getType()));
    }

}
