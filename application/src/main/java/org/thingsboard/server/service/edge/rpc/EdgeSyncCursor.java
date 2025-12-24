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
package org.thingsboard.server.service.edge.rpc;

import lombok.Getter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.AdminSettingsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AiModelEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AssetProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AssetsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DashboardsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DefaultProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DevicesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EntityViewsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationRuleEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationTargetEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationTemplateEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.OAuth2EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.OtaPackagesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.QueuesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantAdminUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantResourcesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class EdgeSyncCursor {

    private final List<EdgeEventFetcher> fetchers = new LinkedList<>();

    @Getter
    private int currentIdx = 0;

    public EdgeSyncCursor(EdgeContextComponent ctx, Edge edge, boolean fullSync) {
        if (fullSync) {
            fetchers.add(new TenantEdgeEventFetcher(ctx.getTenantService()));
            fetchers.add(new QueuesEdgeEventFetcher(ctx.getQueueService()));
            fetchers.add(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService()));
            fetchers.add(new TenantAdminUsersEdgeEventFetcher(ctx.getUserService()));
            fetchers.add(new OAuth2EdgeEventFetcher(ctx.getDomainService()));
            fetchers.add(new SystemWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new TenantWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new AiModelEdgeEventFetcher(ctx.getAiModelService()));
        }
        Customer publicCustomer = ctx.getCustomerService().findPublicCustomer(edge.getTenantId());
        if (publicCustomer != null) {
            fetchers.add(new CustomerEdgeEventFetcher(publicCustomer.getId()));
        }
        if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
            fetchers.add(new CustomerEdgeEventFetcher(edge.getCustomerId()));
            fetchers.add(new CustomerUsersEdgeEventFetcher(ctx.getUserService(), edge.getCustomerId()));
        }
        fetchers.add(new DashboardsEdgeEventFetcher(ctx.getDashboardService()));
        fetchers.add(new DefaultProfilesEdgeEventFetcher(ctx.getDeviceProfileService(), ctx.getAssetProfileService()));
        fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
        fetchers.add(new AssetProfilesEdgeEventFetcher(ctx.getAssetProfileService()));
        fetchers.add(new DevicesEdgeEventFetcher(ctx.getDeviceService()));
        fetchers.add(new AssetsEdgeEventFetcher(ctx.getAssetService()));
        fetchers.add(new EntityViewsEdgeEventFetcher(ctx.getEntityViewService()));
        if (fullSync) {
            fetchers.add(new NotificationTemplateEdgeEventFetcher(ctx.getNotificationTemplateService()));
            fetchers.add(new NotificationTargetEdgeEventFetcher(ctx.getNotificationTargetService()));
            fetchers.add(new NotificationRuleEdgeEventFetcher(ctx.getNotificationRuleService()));
            fetchers.add(new OtaPackagesEdgeEventFetcher(ctx.getOtaPackageService()));
            // sync device profiles twice to update software and hardware fields
            fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
            fetchers.add(new TenantResourcesEdgeEventFetcher(ctx.getResourceService()));
        }
    }

    public boolean hasNext() {
        return fetchers.size() > currentIdx;
    }

    public EdgeEventFetcher getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        EdgeEventFetcher edgeEventFetcher = fetchers.get(currentIdx);
        currentIdx++;
        return edgeEventFetcher;
    }

}
