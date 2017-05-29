/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;

public interface DashboardService {
    
    public Dashboard findDashboardById(DashboardId dashboardId);

    public ListenableFuture<Dashboard> findDashboardByIdAsync(DashboardId dashboardId);

    public DashboardInfo findDashboardInfoById(DashboardId dashboardId);

    public ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(DashboardId dashboardId);

    public Dashboard saveDashboard(Dashboard dashboard);
    
    public Dashboard assignDashboardToCustomer(DashboardId dashboardId, CustomerId customerId);

    public Dashboard unassignDashboardFromCustomer(DashboardId dashboardId);

    public void deleteDashboard(DashboardId dashboardId);
    
    public TextPageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, TextPageLink pageLink);

    public void deleteDashboardsByTenantId(TenantId tenantId);
    
    public TextPageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink);

    public void unassignCustomerDashboards(TenantId tenantId, CustomerId customerId);
    
}
