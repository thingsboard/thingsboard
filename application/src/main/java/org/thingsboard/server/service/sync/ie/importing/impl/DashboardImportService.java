/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.utils.MiscUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DashboardImportService extends BaseEntityImportService<DashboardId, Dashboard, EntityExportData<Dashboard>> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.DASHBOARD, EntityType.DEVICE, EntityType.ASSET));

    private final DashboardService dashboardService;


    @Override
    protected void setOwner(TenantId tenantId, Dashboard dashboard, IdProvider idProvider) {
        dashboard.setTenantId(tenantId);
    }

    @Override
    protected Dashboard findExistingEntity(EntitiesImportCtx ctx, Dashboard dashboard, IdProvider idProvider) {
        Dashboard existingDashboard = super.findExistingEntity(ctx, dashboard, idProvider);
        if (existingDashboard == null && ctx.isFindExistingByName()) {
            existingDashboard = dashboardService.findTenantDashboardsByTitle(ctx.getTenantId(), dashboard.getName()).stream().findFirst().orElse(null);
        }
        return existingDashboard;
    }

    @Override
    protected Dashboard prepare(EntitiesImportCtx ctx, Dashboard dashboard, Dashboard old, EntityExportData<Dashboard> exportData, IdProvider idProvider) {
        for (JsonNode entityAlias : dashboard.getEntityAliasesConfig()) {
            replaceIdsRecursively(ctx, idProvider, entityAlias, Collections.emptySet(), HINTS);
        }
        for (JsonNode widgetConfig : dashboard.getWidgetsConfig()) {
            replaceIdsRecursively(ctx, idProvider, JacksonUtil.getSafely(widgetConfig, "config", "actions"), Collections.singleton("id"), HINTS);
        }
        for (JsonNode entityAlias : dashboard.getEntityAliasesConfig()) {
            MiscUtils.updateDashboardFilterIfRequired(entityAlias);
        }
        return dashboard;
    }

    @Override
    protected Dashboard saveOrUpdate(EntitiesImportCtx ctx, Dashboard dashboard, EntityExportData<Dashboard> exportData, IdProvider idProvider) {
        var tenantId = ctx.getTenantId();

        Set<ShortCustomerInfo> assignedCustomers = Optional.ofNullable(dashboard.getAssignedCustomers()).orElse(Collections.emptySet()).stream()
                .peek(customerInfo -> customerInfo.setCustomerId(idProvider.getInternalId(customerInfo.getCustomerId())))
                .collect(Collectors.toSet());

        if (dashboard.getId() == null) {
            dashboard.setAssignedCustomers(assignedCustomers);
            dashboard = dashboardService.saveDashboard(dashboard);
            for (ShortCustomerInfo customerInfo : assignedCustomers) {
                dashboard = dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerInfo.getCustomerId());
            }
        } else {
            Set<CustomerId> existingAssignedCustomers = Optional.ofNullable(dashboardService.findDashboardById(tenantId, dashboard.getId()).getAssignedCustomers())
                    .orElse(Collections.emptySet()).stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());
            Set<CustomerId> newAssignedCustomers = assignedCustomers.stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());

            Set<CustomerId> toUnassign = new HashSet<>(existingAssignedCustomers);
            toUnassign.removeAll(newAssignedCustomers);
            for (CustomerId customerId : toUnassign) {
                assignedCustomers = dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), customerId).getAssignedCustomers();
            }

            Set<CustomerId> toAssign = new HashSet<>(newAssignedCustomers);
            toAssign.removeAll(existingAssignedCustomers);
            for (CustomerId customerId : toAssign) {
                assignedCustomers = dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId).getAssignedCustomers();
            }
            dashboard.setAssignedCustomers(assignedCustomers);
            dashboard = dashboardService.saveDashboard(dashboard);
        }
        return dashboard;
    }

    @Override
    protected Dashboard deepCopy(Dashboard dashboard) {
        return new Dashboard(dashboard);
    }

    @Override
    protected boolean compare(EntitiesImportCtx ctx, EntityExportData<Dashboard> exportData, Dashboard prepared, Dashboard existing) {
        return super.compare(ctx, exportData, prepared, existing) || !prepared.getConfiguration().equals(existing.getConfiguration());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
