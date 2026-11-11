// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.collections4.CollectionUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class EntitiesRelatedEntityIdAsyncLoader {

    private EntitiesRelatedEntityIdAsyncLoader() {}

    public static ListenableFuture<EntityId> findEntityAsync(
            TbContext ctx,
            EntityId originator,
            RelationsQuery relationsQuery
    ) {
        var relationService = ctx.getRelationService();
        var query = buildQuery(originator, relationsQuery);
        var relationListFuture = relationService.findByQuery(ctx.getTenantId(), query);
        if (relationsQuery.getDirection() == EntitySearchDirection.FROM) {
            return Futures.transform(
                    relationListFuture,
                    relationList -> CollectionUtils.isNotEmpty(relationList) ? relationList.get(0).getTo() : null,
                    ctx.getDbCallbackExecutor()
            );
        } else if (relationsQuery.getDirection() == EntitySearchDirection.TO) {
            return Futures.transform(
                    relationListFuture,
                    relationList -> CollectionUtils.isNotEmpty(relationList) ? relationList.get(0).getFrom() : null,
                    ctx.getDbCallbackExecutor()
            );
        }
        return Futures.immediateFailedFuture(new IllegalStateException("Unknown direction"));
    }

    public static ListenableFuture<List<EntityId>> findEntitiesAsync(TbContext ctx, EntityId originator,
                                                                     RelationsQuery relationsQuery) {
        return findEntitiesAsync(ctx, originator, relationsQuery, entityId -> true);
    }

    public static ListenableFuture<List<EntityId>> findEntitiesAsync(TbContext ctx, EntityId originator,
                                                                     RelationsQuery relationsQuery, Predicate<EntityId> entityFilter) {
        RelationService relationService = ctx.getRelationService();
        EntityRelationsQuery query = buildQuery(originator, relationsQuery);
        ListenableFuture<List<EntityRelation>> asyncRelation = relationService.findByQuery(ctx.getTenantId(), query);

        Function<EntityRelation, EntityId> mapFunction;

        if (relationsQuery.getDirection() == EntitySearchDirection.FROM) {
            mapFunction = EntityRelation::getTo;
        } else if (relationsQuery.getDirection() == EntitySearchDirection.TO) {
            mapFunction = EntityRelation::getFrom;
        } else {
            return Futures.immediateFailedFuture(new IllegalStateException("Unknown direction"));
        }

        return Futures.transform(asyncRelation, r -> CollectionUtils.isNotEmpty(r)
                ? r.stream().map(mapFunction).filter(entityFilter).toList()
                : Collections.emptyList(), ctx.getDbCallbackExecutor());
    }

    private static EntityRelationsQuery buildQuery(EntityId originator, RelationsQuery relationsQuery) {
        var query = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                originator,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }

}
