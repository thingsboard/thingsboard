/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.EnrichmentNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.id.*;

@EnrichmentNode(name="Get Customer Attributes Node")
public class TbGetCustomerAttributeNode extends TbEntityGetAttrNode<CustomerId> {

    @Override
    protected ListenableFuture<CustomerId> findEntityAsync(TbContext ctx, EntityId originator) {

        switch (originator.getEntityType()) {
            case CUSTOMER:
                return Futures.immediateFuture((CustomerId) originator);
            case USER:
                return getCustomerAsync(ctx.getUserService().findUserByIdAsync((UserId) originator));
            case ASSET:
                return getCustomerAsync(ctx.getAssetService().findAssetByIdAsync((AssetId) originator));
            case DEVICE:
                return getCustomerAsync(ctx.getDeviceService().findDeviceByIdAsync((DeviceId) originator));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected originator EntityType " + originator));
        }
    }

    private <T extends HasCustomerId> ListenableFuture<CustomerId> getCustomerAsync(ListenableFuture<T> future) {
        return Futures.transform(future, (AsyncFunction<HasCustomerId, CustomerId>) in -> {
            return in != null ? Futures.immediateFuture(in.getCustomerId())
                    : Futures.immediateFailedFuture(new IllegalStateException("Customer not found"));});
    }

}
