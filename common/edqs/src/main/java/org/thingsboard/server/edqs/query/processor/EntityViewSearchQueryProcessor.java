// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityViewSearchQueryFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

public class EntityViewSearchQueryProcessor extends AbstractEntitySearchQueryProcessor<EntityViewSearchQueryFilter> {

    public EntityViewSearchQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityViewSearchQueryFilter) query.getEntityFilter());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }

    @Override
    protected boolean check(RelationInfo relationInfo) {
        EntityData<?> ed = relationInfo.getTarget();
        return super.check(relationInfo) &&
                (filter.getEntityViewTypes() == null || filter.getEntityViewTypes().contains(ed.getFields().getType()));
    }

}
