// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.List;

public class EntityViewTypeQueryProcessor extends AbstractEntityProfileNameQueryProcessor<EntityViewTypeFilter> {

    public EntityViewTypeQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityViewTypeFilter) query.getEntityFilter(), EntityType.ENTITY_VIEW);
    }

    @Override
    protected String getEntityNameFilter(EntityViewTypeFilter filter) {
        return filter.getEntityViewNameFilter();
    }

    @Override
    protected List<String> getProfileNames(EntityViewTypeFilter filter) {
        return filter.getEntityViewTypes();
    }

}
