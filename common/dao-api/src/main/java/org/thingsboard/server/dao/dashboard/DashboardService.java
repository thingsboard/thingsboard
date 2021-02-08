/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;

public interface DashboardService {
    
    Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId);

    DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId);

    Dashboard saveDashboard(Dashboard dashboard);

    Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    void deleteDashboard(TenantId tenantId, DashboardId dashboardId);

    PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    void deleteDashboardsByTenantId(TenantId tenantId);

    PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    void unassignCustomerDashboards(TenantId tenantId, CustomerId customerId);

    void updateCustomerDashboards(TenantId tenantId, CustomerId customerId);

}
