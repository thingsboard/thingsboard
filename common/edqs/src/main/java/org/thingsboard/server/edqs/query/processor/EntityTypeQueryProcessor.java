// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

public class EntityTypeQueryProcessor extends AbstractSimpleQueryProcessor<EntityTypeFilter> {

    public EntityTypeQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityTypeFilter) query.getEntityFilter(), ((EntityTypeFilter) query.getEntityFilter()).getEntityType());
    }

    @Override
    protected boolean matches(EntityData ed) {
        return super.matches(ed);
    }

}
