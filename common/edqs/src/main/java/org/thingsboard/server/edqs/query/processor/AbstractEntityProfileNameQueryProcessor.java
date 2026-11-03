// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class AbstractEntityProfileNameQueryProcessor<T extends EntityFilter> extends AbstractSimpleQueryProcessor<T> {

    private final Set<String> entityProfileNames;
    private final Pattern pattern;

    public AbstractEntityProfileNameQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query, T filter, EntityType entityType) {
        super(repo, ctx, query, filter, entityType);
        entityProfileNames = new HashSet<>(getProfileNames(this.filter));
        pattern = RepositoryUtils.toEntityNameSqlLikePattern(getEntityNameFilter(filter));
    }

    protected abstract String getEntityNameFilter(T filter);

    protected abstract List<String> getProfileNames(T filter);

    @Override
    protected boolean matches(EntityData<?> ed) {
        return super.matches(ed) && entityProfileNames.contains(ed.getFields().getType())
                && (pattern == null || pattern.matcher(ed.getFields().getName()).matches());
    }

}
