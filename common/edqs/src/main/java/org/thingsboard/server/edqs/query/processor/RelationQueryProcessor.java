// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RelationQueryProcessor extends AbstractRelationQueryProcessor<RelationsQueryFilter> {

    private final boolean hasFilters;

    public RelationQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (RelationsQueryFilter) query.getEntityFilter());
        this.hasFilters = filter.getFilters() != null && !filter.getFilters().isEmpty();
    }

    @Override
    public Set<UUID> getRootEntities() {
        if (filter.isMultiRoot()) {
            return filter.getMultiRootEntityIds().stream().map(UUID::fromString).collect(Collectors.toSet());
        } else {
            return Set.of(filter.getRootEntity().getId());
        }
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
    public boolean isMultiRoot() {
        return filter.isMultiRoot();
    }

    @Override
    public boolean isFetchLastLevelOnly() {
        return filter.isFetchLastLevelOnly();
    }

    @Override
    protected boolean check(RelationInfo relationInfo) {
        if (hasFilters) {
            for (var f : filter.getFilters()) {
                if (((!filter.isNegate() && !f.isNegate()) || (filter.isNegate() && f.isNegate())) == f.getRelationType().equals(relationInfo.getType())) {
                    if (f.getEntityTypes() == null || f.getEntityTypes().isEmpty()
                            || f.getEntityTypes().contains(relationInfo.getTarget().getEntityType())) {
                        return super.matches(relationInfo.getTarget());
                    }
                }
            }
            return false;
        } else {
            return super.matches(relationInfo.getTarget());
        }
    }

}
