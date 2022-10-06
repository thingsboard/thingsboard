/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

public class EntitiesTenantIdAsyncLoader {

    public static ListenableFuture<TenantId> findEntityIdAsync(TbContext ctx, EntityId entityId) {

        switch (entityId.getEntityType()) {
            case TENANT:
                return Futures.immediateFuture((TenantId) entityId);
            case CUSTOMER:
                return getTenantAsync(ctx, ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), (CustomerId) entityId));
            case USER:
                return getTenantAsync(ctx, ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) entityId));
            case ASSET:
                return getTenantAsync(ctx, ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) entityId));
            case DEVICE:
                return getTenantAsync(ctx, ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), (DeviceId) entityId));
            case ALARM:
                return getTenantAsync(ctx, ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), (AlarmId) entityId));
            case RULE_CHAIN:
                return getTenantAsync(ctx, ctx.getRuleChainService().findRuleChainByIdAsync(ctx.getTenantId(), (RuleChainId) entityId));
            case ENTITY_VIEW:
                return getTenantAsync(ctx, ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), (EntityViewId) entityId));
            case DASHBOARD:
                return getTenantAsync(ctx, ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), (DashboardId) entityId));
            case EDGE:
                return getTenantAsync(ctx, ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), (EdgeId) entityId));
            case OTA_PACKAGE:
                return getTenantAsync(ctx, ctx.getOtaPackageService().findOtaPackageInfoByIdAsync(ctx.getTenantId(), (OtaPackageId) entityId));
            case ASSET_PROFILE:
                return getTenantAsync(ctx, Futures.immediateFuture(ctx.getAssetProfileCache().get(ctx.getTenantId(), (AssetProfileId) entityId)));
            case DEVICE_PROFILE:
                return getTenantAsync(ctx, Futures.immediateFuture(ctx.getDeviceProfileCache().get(ctx.getTenantId(), (DeviceProfileId) entityId)));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected entityId EntityType " + entityId.getEntityType()));
        }
    }

    private static <T extends HasTenantId> ListenableFuture<TenantId> getTenantAsync(TbContext ctx, ListenableFuture<T> future) {
        return Futures.transformAsync(future, in -> {
            return in != null ? Futures.immediateFuture(in.getTenantId())
                    : Futures.immediateFuture(null);
        }, ctx.getDbCallbackExecutor());
    }
}
