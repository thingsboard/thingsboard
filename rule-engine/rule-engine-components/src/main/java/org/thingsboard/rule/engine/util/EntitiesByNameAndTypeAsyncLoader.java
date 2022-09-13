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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Optional;

public class EntitiesByNameAndTypeAsyncLoader {

    public static ListenableFuture<? extends EntityId> findEntityIdAsync(TbContext ctx, EntityType entityType, String entityName) {
        EntityId targetEntity = null;
        switch (entityType) {
            case DEVICE:
                Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), entityName);
                if (device != null) {
                    targetEntity = device.getId();
                }
                break;
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), entityName);
                if (asset != null) {
                    targetEntity = asset.getId();
                }
                break;
            case CUSTOMER:
                Optional<Customer> customerOptional = ctx.getCustomerService().findCustomerByTenantIdAndTitle(ctx.getTenantId(), entityName);
                if (customerOptional.isPresent()) {
                    targetEntity = customerOptional.get().getId();
                }
                break;
            case TENANT:
                targetEntity = ctx.getTenantId();
                break;
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewByTenantIdAndName(ctx.getTenantId(), entityName);
                if (entityView != null) {
                    targetEntity = entityView.getId();
                }
                break;
            case EDGE:
                Edge edge = ctx.getEdgeService().findEdgeByTenantIdAndName(ctx.getTenantId(), entityName);
                if (edge != null) {
                    targetEntity = edge.getId();
                }
                break;
            case DASHBOARD:
                PageData<DashboardInfo> dashboardInfoTextPageData = ctx.getDashboardService().findDashboardsByTenantId(ctx.getTenantId(), new PageLink(200, 0, entityName));
                Optional<DashboardInfo> currentDashboardInfo = dashboardInfoTextPageData.getData().stream()
                        .filter(dashboardInfo -> dashboardInfo.getTitle().equals(entityName))
                        .findFirst();
                if (currentDashboardInfo.isPresent()) {
                    targetEntity = currentDashboardInfo.get().getId();
                }
                break;
            case USER:
                User user = ctx.getUserService().findUserByEmail(ctx.getTenantId(), entityName);
                if (user != null) {
                    targetEntity = user.getId();
                }
                break;
            default:
                return Futures.immediateFailedFuture(new IllegalStateException("Unexpected entity type " + entityType.name()));
        }

        if (targetEntity != null) {
            return Futures.immediateFuture(targetEntity);
        } else {
            return Futures.immediateFailedFuture(new IllegalStateException("Failed to found entity " + entityType.name() + " by name '" + entityName + "'!"));
        }
    }

}
