// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Data
public class ParentEntitiesRelationsQuery implements ParentEntitiesQuery {

    private EntityId rootEntityId;
    private RelationsQuery relationsQuery;
    private RelationsQuery childRelationsQuery;
    private boolean includeRootEntity;

    @Override
    public ListenableFuture<List<EntityId>> getParentEntitiesAsync(TbContext ctx) {
        return getParentEntitiesAsyncInternal(ctx, entityId -> true);
    }

    @Override
    public ListenableFuture<List<EntityId>> getLocalParentEntitiesAsync(TbContext ctx) {
        return getParentEntitiesAsyncInternal(ctx, entityId -> ctx.getPeContext().isLocalEntity(entityId));
    }

    private ListenableFuture<List<EntityId>> getParentEntitiesAsyncInternal(TbContext ctx, Predicate<EntityId> filter) {
        ListenableFuture<List<EntityId>> parentEntities = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, rootEntityId, relationsQuery, filter);
        if (includeRootEntity) {
            return Futures.transform(parentEntities, entityIds -> {
                List<EntityId> newEntityIds = new ArrayList<>(entityIds);
                if (!newEntityIds.contains(rootEntityId)) {
                    newEntityIds.add(rootEntityId);
                }
                return newEntityIds;
            }, ctx.getDbCallbackExecutor());
        }
        return parentEntities;
    }

    @Override
    public ListenableFuture<List<EntityId>> getChildEntitiesAsync(TbContext ctx, EntityId parentEntityId) {
        return EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, parentEntityId, childRelationsQuery);
    }

    @Override
    public boolean useParentEntitiesOnlyForSimpleAggregation() {
        return true;
    }

}
