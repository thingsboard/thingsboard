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
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityFieldsData;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.function.Function;

public class EntitiesFieldsAsyncLoader {

    public static ListenableFuture<EntityFieldsData> findAsync(TbContext ctx, EntityId original) {
        switch (original.getEntityType()) {
            case TENANT:
                return getAsync(ctx.getTenantService().findTenantByIdAsync((TenantId) original),
                        t -> new EntityFieldsData(t));
            case CUSTOMER:
                return getAsync(ctx.getCustomerService().findCustomerByIdAsync((CustomerId) original),
                        t -> new EntityFieldsData(t));
            case USER:
                return getAsync(ctx.getUserService().findUserByIdAsync((UserId) original),
                        t -> new EntityFieldsData(t));
            case ASSET:
                return getAsync(ctx.getAssetService().findAssetByIdAsync((AssetId) original),
                        t -> new EntityFieldsData(t));
            case DEVICE:
                return getAsync(ctx.getDeviceService().findDeviceByIdAsync((DeviceId) original),
                        t -> new EntityFieldsData(t));
            case ALARM:
                return getAsync(ctx.getAlarmService().findAlarmByIdAsync((AlarmId) original),
                        t -> new EntityFieldsData(t));
            case RULE_CHAIN:
                return getAsync(ctx.getRuleChainService().findRuleChainByIdAsync((RuleChainId) original),
                        t -> new EntityFieldsData(t));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original));
        }
    }

    private static <T extends BaseData> ListenableFuture<EntityFieldsData> getAsync(
            ListenableFuture<T> future, Function<T, EntityFieldsData> converter) {
        return Futures.transformAsync(future, in -> in != null ?
                Futures.immediateFuture(converter.apply(in))
                : Futures.immediateFailedFuture(new RuntimeException("Entity not found!")));
    }
}
