// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Set;
import java.util.UUID;

public abstract class AbstractEntitySearchQueryProcessor<T extends EntitySearchQueryFilter> extends AbstractRelationQueryProcessor<T> {


    public AbstractEntitySearchQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query, T filter) {
        super(repo, ctx, query, filter);
    }

    @Override
    public Set<UUID> getRootEntities() {
        return Set.of(filter.getRootEntity().getId());
    }

    @Override
    public EntitySearchDirection getDirection() {
        return filter.getDirection();
    }

    @Override
    public int getMaxLevel() {
        return filter.getMaxLevel();
    }

    @Override
    public boolean isFetchLastLevelOnly() {
        return filter.isFetchLastLevelOnly();
    }

    public abstract EntityType getEntityType();

    @Override
    protected boolean check(RelationInfo relationInfo) {
        EntityData<?> target = relationInfo.getTarget();
        return (filter.getRelationType() == null || relationInfo.getType().equals(filter.getRelationType())) &&
                getEntityType().equals(target.getEntityType()) && super.matches(target);
    }

}
