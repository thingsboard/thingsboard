// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.regex.Pattern;

public class EntityNameQueryProcessor extends AbstractSimpleQueryProcessor<EntityNameFilter> {

    private final Pattern pattern;

    public EntityNameQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityNameFilter) query.getEntityFilter(), ((EntityNameFilter) query.getEntityFilter()).getEntityType());
        pattern = RepositoryUtils.toEntityNameSqlLikePattern(filter.getEntityNameFilter());
    }

    @Override
    protected boolean matches(EntityData ed) {
        return super.matches(ed) && (pattern == null || pattern.matcher(ed.getFields().getName()).matches());
    }

}
