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
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
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
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.util.UUID;

public class EntitiesTenantIdAsyncLoader {

    public static ListenableFuture<TenantId> findEntityIdAsync(TbContext ctx, EntityId original) {
        ListenableFuture<? extends HasTenantId> hasTenantId;
        UUID id = original.getId();
        EntityType entityType = original.getEntityType();
        TenantId tenantId = ctx.getTenantId();
        switch (entityType) {
            case TENANT:
                return Futures.immediateFuture(new TenantId(id));
            case CUSTOMER:
                hasTenantId = ctx.getCustomerService().findCustomerByIdAsync(tenantId, new CustomerId(id));
                break;
            case USER:
                hasTenantId = ctx.getUserService().findUserByIdAsync(tenantId, new UserId(id));
                break;
            case ASSET:
                hasTenantId = ctx.getAssetService().findAssetByIdAsync(tenantId, new AssetId(id));
                break;
            case DEVICE:
                hasTenantId = ctx.getDeviceService().findDeviceByIdAsync(tenantId, new DeviceId(id));
                break;
            case ALARM:
                hasTenantId = ctx.getAlarmService().findAlarmByIdAsync(tenantId, new AlarmId(id));
                break;
            case RULE_CHAIN:
                hasTenantId = ctx.getRuleChainService().findRuleChainByIdAsync(tenantId, new RuleChainId(id));
                break;
            case ENTITY_VIEW:
                hasTenantId = ctx.getEntityViewService().findEntityViewByIdAsync(tenantId, new EntityViewId(id));
                break;
            case DASHBOARD:
                hasTenantId = ctx.getDashboardService().findDashboardByIdAsync(tenantId, new DashboardId(id));
                break;
            case EDGE:
                hasTenantId = ctx.getEdgeService().findEdgeByIdAsync(tenantId, new EdgeId(id));
                break;
            case OTA_PACKAGE:
                hasTenantId = ctx.getOtaPackageService().findOtaPackageInfoByIdAsync(tenantId, new OtaPackageId(id));
                break;
            case ASSET_PROFILE:
                hasTenantId = Futures.immediateFuture(ctx.getAssetProfileCache().get(tenantId, new AssetProfileId(id)));
                break;
            case DEVICE_PROFILE:
                hasTenantId = Futures.immediateFuture(ctx.getDeviceProfileCache().get(tenantId, new DeviceProfileId(id)));
                break;
            case WIDGET_TYPE:
                hasTenantId = Futures.immediateFuture(ctx.getWidgetTypeService().findWidgetTypeById(tenantId, new WidgetTypeId(id)));
                break;
            case WIDGETS_BUNDLE:
                hasTenantId = Futures.immediateFuture(ctx.getWidgetBundleService().findWidgetsBundleById(tenantId, new WidgetsBundleId(id)));
                break;
            case RPC:
                hasTenantId = ctx.getRpcService().findRpcByIdAsync(tenantId, new RpcId(id));
                break;
            case QUEUE:
                hasTenantId = Futures.immediateFuture(ctx.getQueueService().findQueueById(tenantId, new QueueId(id)));
                break;
            case API_USAGE_STATE:
                hasTenantId = Futures.immediateFuture(ctx.getRuleEngineApiUsageStateService().findApiUsageStateById(tenantId, new ApiUsageStateId(id)));
                break;
            case TB_RESOURCE:
                hasTenantId = ctx.getResourceService().findResourceInfoByIdAsync(tenantId, new TbResourceId(id));
                break;
            case RULE_NODE:
                RuleNode ruleNode = ctx.getRuleChainService().findRuleNodeById(tenantId, new RuleNodeId(id));
                if (ruleNode != null) {
                    hasTenantId = ctx.getRuleChainService().findRuleChainByIdAsync(tenantId, ruleNode.getRuleChainId());
                } else {
                    hasTenantId = Futures.immediateFuture(null);
                }
                break;
            case TENANT_PROFILE:
                if (ctx.getTenantProfile().getId().equals(id)) {
                    return Futures.immediateFuture(tenantId);
                } else {
                    hasTenantId = Futures.immediateFuture(null);
                }
                break;
            default:
                hasTenantId = Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original.getEntityType()));
        }
        return getTenantIdAsync(hasTenantId, ctx.getDbCallbackExecutor());
    }

    private static <T extends HasTenantId> ListenableFuture<TenantId> getTenantIdAsync(ListenableFuture<T> future, ListeningExecutor executor) {
        return Futures.transformAsync(future, in -> in != null ? Futures.immediateFuture(in.getTenantId())
                : Futures.immediateFuture(null), executor);
    }
}
