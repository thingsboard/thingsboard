// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Collections;
import java.util.List;

@Data
public class ParentEntitiesGroup implements ParentEntitiesQuery {

    private EntityId entityGroupId;

    @Override
    public ListenableFuture<List<EntityId>> getParentEntitiesAsync(TbContext ctx) {
        return Futures.immediateFuture(Collections.singletonList(entityGroupId));
    }

    @Override
    public ListenableFuture<List<EntityId>> getLocalParentEntitiesAsync(TbContext ctx) {
        if (ctx.getPeContext().isLocalEntity(entityGroupId)) {
            return Futures.immediateFuture(Collections.singletonList(entityGroupId));
        } else {
            return Futures.immediateFuture(Collections.emptyList());
        }
    }

    @Override
    public ListenableFuture<List<EntityId>> getChildEntitiesAsync(TbContext ctx, EntityId parentEntityId) {
        return ctx.getPeContext().getEntityGroupService().findAllEntityIdsAsync(ctx.getTenantId(), new EntityGroupId(parentEntityId.getId()),
                new PageLink(Integer.MAX_VALUE));
    }

    @Override
    public boolean useParentEntitiesOnlyForSimpleAggregation() {
        return false;
    }

}
