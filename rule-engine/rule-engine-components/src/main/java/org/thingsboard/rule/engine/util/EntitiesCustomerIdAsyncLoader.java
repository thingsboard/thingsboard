/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.UserId;

public class EntitiesCustomerIdAsyncLoader {

    public static ListenableFuture<CustomerId> findEntityCustomerIdAsync(TbContext ctx, EntityId originator) {
        var tenantId = ctx.getTenantId();
        return switch (originator.getEntityType()) {
            case CUSTOMER -> Futures.immediateFuture((CustomerId) originator);
            case USER -> toCustomerIdAsync(ctx, ctx.getUserService().findUserByIdAsync(tenantId, (UserId) originator));
            case ASSET -> toCustomerIdAsync(ctx, ctx.getAssetService().findAssetByIdAsync(tenantId, (AssetId) originator));
            case DEVICE -> toCustomerIdAsync(ctx, Futures.immediateFuture(ctx.getDeviceService().findDeviceById(tenantId, (DeviceId) originator)));
            case ENTITY_VIEW -> toCustomerIdAsync(ctx, ctx.getEntityViewService().findEntityViewByIdAsync(tenantId, (EntityViewId) originator));
            case EDGE -> toCustomerIdAsync(ctx, ctx.getEdgeService().findEdgeByIdAsync(tenantId, (EdgeId) originator));
            default -> Futures.immediateFailedFuture(new TbNodeException("Unexpected originator EntityType: " + originator.getEntityType()));
        };
    }

    private static <T extends HasCustomerId> ListenableFuture<CustomerId> toCustomerIdAsync(TbContext ctx, ListenableFuture<T> future) {
        return Futures.transform(future, in -> in != null ? in.getCustomerId() : null, ctx.getDbCallbackExecutor());
    }

}
