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
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.Set;

@Slf4j
public abstract class BaseDashboardProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg, CustomerId customerId) {
        boolean created = false;
        Dashboard dashboard = constructDashboardFromUpdateMsg(tenantId, dashboardId, dashboardUpdateMsg);
        if (dashboard == null) {
            throw new RuntimeException("[{" + tenantId + "}] dashboardUpdateMsg {" + dashboardUpdateMsg + "} cannot be converted to dashboard");
        }
        Set<ShortCustomerInfo> assignedCustomers = null;
        Dashboard dashboardById = dashboardService.findDashboardById(tenantId, dashboardId);
        if (dashboardById == null) {
            created = true;
            dashboard.setId(null);
        } else {
            dashboard.setId(dashboardId);
            assignedCustomers = filterNonExistingCustomers(tenantId, dashboardById.getAssignedCustomers());
        }

        dashboardValidator.validate(dashboard, Dashboard::getTenantId);
        if (created) {
            dashboard.setId(dashboardId);
        }
        Set<ShortCustomerInfo> msgAssignedCustomers = filterNonExistingCustomers(tenantId, dashboard.getAssignedCustomers());
        if (msgAssignedCustomers != null) {
            if (assignedCustomers == null) {
                assignedCustomers = msgAssignedCustomers;
            } else {
                assignedCustomers.addAll(msgAssignedCustomers);
            }
        }
        dashboard.setAssignedCustomers(assignedCustomers);
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard, false);
        if (msgAssignedCustomers != null && !msgAssignedCustomers.isEmpty()) {
            for (ShortCustomerInfo assignedCustomer : msgAssignedCustomers) {
                if (assignedCustomer.getCustomerId().equals(customerId)) {
                    dashboardService.assignDashboardToCustomer(tenantId, savedDashboard.getId(), assignedCustomer.getCustomerId());
                }
            }
        } else {
            unassignCustomersFromDashboard(tenantId, savedDashboard, customerId);
        }
        return created;
    }

    private void unassignCustomersFromDashboard(TenantId tenantId, Dashboard dashboard, CustomerId customerId) {
        if (dashboard.getAssignedCustomers() != null && !dashboard.getAssignedCustomers().isEmpty()) {
            for (ShortCustomerInfo assignedCustomer : dashboard.getAssignedCustomers()) {
                if (assignedCustomer.getCustomerId().equals(customerId)) {
                    dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), assignedCustomer.getCustomerId());
                }
            }
        }
    }

    protected abstract Dashboard constructDashboardFromUpdateMsg(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg);

    protected abstract Set<ShortCustomerInfo> filterNonExistingCustomers(TenantId tenantId, Set<ShortCustomerInfo> assignedCustomers);
}
