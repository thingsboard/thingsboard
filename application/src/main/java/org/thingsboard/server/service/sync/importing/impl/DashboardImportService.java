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
package org.thingsboard.server.service.sync.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.exporting.data.DashboardExportData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DashboardImportService extends BaseEntityImportService<DashboardId, Dashboard, DashboardExportData> {

    private final DashboardService dashboardService;

    // TODO [viacheslav]: improve the code
    @Override
    protected Dashboard prepareAndSave(TenantId tenantId, Dashboard dashboard, DashboardExportData exportData, NewIdProvider idProvider) {
        dashboard.setTenantId(tenantId);
        if (dashboard.getId() == null) {
            Set<ShortCustomerInfo> assignedCustomers = idProvider.get(tenantId, Dashboard::getAssignedCustomers, ShortCustomerInfo::getCustomerId, ShortCustomerInfo::setCustomerId);
            dashboard.setAssignedCustomers(null);
            dashboard = dashboardService.saveDashboard(dashboard);
            for (ShortCustomerInfo customerInfo : assignedCustomers) {
                dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerInfo.getCustomerId());
            }
        } else {
            Set<CustomerId> existingAssignedCustomers = Optional.ofNullable(dashboardService.findDashboardById(tenantId, dashboard.getId()).getAssignedCustomers())
                    .orElse(Collections.emptySet()).stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());
            Set<CustomerId> newAssignedCustomers = idProvider.get(tenantId, Dashboard::getAssignedCustomers, ShortCustomerInfo::getCustomerId, ShortCustomerInfo::setCustomerId).stream()
                    .map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());

            Set<CustomerId> toUnassign = new HashSet<>(existingAssignedCustomers);
            toUnassign.removeAll(newAssignedCustomers);
            for (CustomerId customerId : toUnassign) {
                dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), customerId);
            }
            Set<CustomerId> toAssign = new HashSet<>(newAssignedCustomers);
            toAssign.removeAll(existingAssignedCustomers);
            for (CustomerId customerId : toAssign) {
                dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId);
            }
            dashboard.setAssignedCustomers(dashboardService.findDashboardById(tenantId, dashboard.getId()).getAssignedCustomers());
            dashboard = dashboardService.saveDashboard(dashboard);
        }
        return dashboard;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
