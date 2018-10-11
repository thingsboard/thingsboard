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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasMultipleCustomers;
import org.thingsboard.server.common.data.id.*;

public class EntitiesCustomerIdAsyncLoader {


    public static ListenableFuture<CustomerId> findEntityIdAsync(TbContext ctx, EntityId original) {

        switch (original.getEntityType()) {
            case CUSTOMER:
                return Futures.immediateFuture((CustomerId) original);
            case USER:
                return getCustomerAsync(ctx.getUserService().findUserByIdAsync((UserId) original));
            case ASSET:
                return getCustomerAsyncFromMultipleCustomers(ctx.getAssetService().findAssetByIdAsync((AssetId) original));
            case DEVICE:
                return getCustomerAsyncFromMultipleCustomers(ctx.getDeviceService().findDeviceByIdAsync((DeviceId) original));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original));
        }
    }

    private static <T extends HasCustomerId> ListenableFuture<CustomerId> getCustomerAsync(ListenableFuture<T> future) {
        return Futures.transformAsync(future, in -> in != null ? Futures.immediateFuture(in.getCustomerId())
                : Futures.immediateFuture(null));
    }

    // TbChangeOriginatorNode description says "Related Entity found using configured relation direction and Relation
    // Type. If multiple Related Entities are found, only first Entity is used as new Originator, other entities are d
    // iscarded." So, returning the first customerId from assigned customers would be ok.
    private static <T extends HasMultipleCustomers> ListenableFuture<CustomerId> getCustomerAsyncFromMultipleCustomers(
            ListenableFuture<T> future) {
        return Futures.transformAsync(future, in -> in != null ?
                Futures.immediateFuture(in.getAssignedCustomers().isEmpty() ? null :
                        in.getAssignedCustomers().iterator().next().getCustomerId()) : Futures.immediateFuture(null));
    }

}
