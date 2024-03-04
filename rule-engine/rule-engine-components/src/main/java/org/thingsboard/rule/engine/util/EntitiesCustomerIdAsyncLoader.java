/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;

public class EntitiesCustomerIdAsyncLoader {

    public static ListenableFuture<CustomerId> findEntityIdAsync(TbContext ctx, EntityId originator) {
        switch (originator.getEntityType()) {
            case CUSTOMER:
                return Futures.immediateFuture((CustomerId) originator);
            case USER:
                return toCustomerIdAsync(ctx, ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) originator));
            case ASSET:
                return toCustomerIdAsync(ctx, ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) originator));
            case DEVICE:
                return toCustomerIdAsync(ctx, Futures.immediateFuture(ctx.getDeviceService().findDeviceById(ctx.getTenantId(), (DeviceId) originator)));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected originator EntityType: " + originator.getEntityType()));
        }
    }

    private static <T extends HasCustomerId> ListenableFuture<CustomerId> toCustomerIdAsync(TbContext ctx, ListenableFuture<T> future) {
        return Futures.transform(future, in -> in != null ? in.getCustomerId() : null, ctx.getDbCallbackExecutor());
    }

}
