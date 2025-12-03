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
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public abstract class BaseDashboardProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<Dashboard> dashboardValidator;

    protected boolean saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg, CustomerId customerId) {
        boolean created = false;
        Dashboard dashboard = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        if (dashboard == null) {
            throw new RuntimeException("[{" + tenantId + "}] dashboardUpdateMsg {" + dashboardUpdateMsg + "} cannot be converted to dashboard");
        }
        Set<ShortCustomerInfo> newAssignedCustomers = new HashSet<>();
        if (dashboard.getAssignedCustomers() != null && !dashboard.getAssignedCustomers().isEmpty()) {
            newAssignedCustomers.addAll(dashboard.getAssignedCustomers());
        }
        Dashboard dashboardById = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        if (dashboardById == null) {
            created = true;
            dashboard.setId(null);
            dashboard.setAssignedCustomers(null);
        } else {
            dashboard.setId(dashboardId);
            dashboard.setAssignedCustomers(dashboardById.getAssignedCustomers());
        }
        if (isSaveRequired(dashboardById, dashboard)) {
            dashboardValidator.validate(dashboard, Dashboard::getTenantId);
            if (created) {
                dashboard.setId(dashboardId);
            }
            Dashboard savedDashboard = edgeCtx.getDashboardService().saveDashboard(dashboard, false);
            updateDashboardAssignments(tenantId, dashboardById, savedDashboard, newAssignedCustomers);
        }
        return created;
    }

    private void updateDashboardAssignments(TenantId tenantId, Dashboard dashboardById, Dashboard savedDashboard, Set<ShortCustomerInfo> newAssignedCustomers) {
        Set<ShortCustomerInfo> currentAssignedCustomers = new HashSet<>();
        if (dashboardById != null) {
            if (dashboardById.getAssignedCustomers() != null) {
                currentAssignedCustomers.addAll(dashboardById.getAssignedCustomers());
            }
        }

        newAssignedCustomers = filterNonExistingCustomers(tenantId, currentAssignedCustomers, newAssignedCustomers);

        Set<CustomerId> addedCustomerIds = new HashSet<>();
        Set<CustomerId> removedCustomerIds = new HashSet<>();
        for (ShortCustomerInfo newAssignedCustomer : newAssignedCustomers) {
            if (!savedDashboard.isAssignedToCustomer(newAssignedCustomer.getCustomerId())) {
                addedCustomerIds.add(newAssignedCustomer.getCustomerId());
            }
        }

        for (ShortCustomerInfo currentAssignedCustomer : currentAssignedCustomers) {
            if (!newAssignedCustomers.contains(currentAssignedCustomer)) {
                removedCustomerIds.add(currentAssignedCustomer.getCustomerId());
            }
        }

        for (CustomerId customerIdToAdd : addedCustomerIds) {
            edgeCtx.getDashboardService().assignDashboardToCustomer(tenantId, savedDashboard.getId(), customerIdToAdd);
        }
        for (CustomerId customerIdToRemove : removedCustomerIds) {
            edgeCtx.getDashboardService().unassignDashboardFromCustomer(tenantId, savedDashboard.getId(), customerIdToRemove);
        }
    }

    protected void deleteDashboard(TenantId tenantId, DashboardId dashboardId) {
        deleteDashboard(tenantId, null, dashboardId);
    }

    protected void deleteDashboard(TenantId tenantId, Edge edge, DashboardId dashboardId) {
        Dashboard dashboardById = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        if (dashboardById != null) {
            edgeCtx.getDashboardService().deleteDashboard(tenantId, dashboardId);
            pushEntityEventToRuleEngine(tenantId, edge, dashboardById, TbMsgType.ENTITY_DELETED);
        }
    }

    protected abstract Set<ShortCustomerInfo> filterNonExistingCustomers(TenantId tenantId, Set<ShortCustomerInfo> currentAssignedCustomers, Set<ShortCustomerInfo> newAssignedCustomers);

}
