// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.ProfileAwareData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public abstract class AbstractEntityProfileQueryProcessor<T extends EntityFilter> extends AbstractSimpleQueryProcessor<T> {

    private final Set<UUID> entityProfileIds = new HashSet<>();
    private final Pattern pattern;

    public AbstractEntityProfileQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query, T filter, EntityType entityType) {
        super(repo, ctx, query, filter, entityType);
        var profileNamesSet = new HashSet<>(getProfileNames(this.filter));
        for (EntityData<?> dp : repo.getEntitySet(getProfileEntityType())) {
            if (profileNamesSet.contains(dp.getFields().getName())) {
                entityProfileIds.add(dp.getId());
            }
        }
        pattern = RepositoryUtils.toEntityNameSqlLikePattern(getEntityNameFilter(filter));
    }

    protected abstract String getEntityNameFilter(T filter);

    protected abstract List<String> getProfileNames(T filter);

    protected abstract EntityType getProfileEntityType();

    @Override
    protected boolean matches(EntityData<?> ed) {
        ProfileAwareData<?> profileAwareData = (ProfileAwareData<?>) ed;
        return super.matches(ed) && entityProfileIds.contains(profileAwareData.getFields().getProfileId())
                && (pattern == null || pattern.matcher(profileAwareData.getFields().getName()).matches());
    }

}
