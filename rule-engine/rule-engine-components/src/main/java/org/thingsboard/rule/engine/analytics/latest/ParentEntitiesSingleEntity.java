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

import java.util.Collections;
import java.util.List;

@Data
public class ParentEntitiesSingleEntity implements ParentEntitiesQuery {

    private EntityId entityId;
    private RelationsQuery childRelationsQuery;

    @Override
    public ListenableFuture<List<EntityId>> getParentEntitiesAsync(TbContext ctx) {
        return Futures.immediateFuture(Collections.singletonList(entityId));
    }

    @Override
    public ListenableFuture<List<EntityId>> getLocalParentEntitiesAsync(TbContext ctx) {
        if (ctx.getPeContext().isLocalEntity(entityId)) {
            return Futures.immediateFuture(Collections.singletonList(entityId));
        } else {
            return Futures.immediateFuture(Collections.emptyList());
        }
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
