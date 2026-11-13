// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface DashboardService extends EntityDaoService {

    Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId);

    DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId);

    String findDashboardTitleById(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<List<DashboardInfo>> findDashboardInfoByIdsAsync(TenantId tenantId, List<DashboardId> dashboardIds);

    List<DashboardInfo> findDashboardInfoByIds(TenantId tenantId, List<DashboardId> dashboardIds);

    Dashboard saveDashboard(Dashboard dashboard, boolean doValidate);

    Dashboard saveDashboard(Dashboard dashboard);

    Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    void deleteDashboard(TenantId tenantId, DashboardId dashboardId);

    PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    Long countDashboards();

    PageData<DashboardInfo> findTenantDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<DashboardInfo> findMobileDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    void deleteDashboardsByTenantId(TenantId tenantId);

    void deleteDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    PageData<DashboardInfo> findDashboardsByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<DashboardInfo> findDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<DashboardInfo> findMobileDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    List<Dashboard> exportDashboards(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink) throws ThingsboardException;

    void importDashboards(TenantId tenantId, EntityGroupId entityGroupId, List<Dashboard> dashboards, boolean overwrite) throws ThingsboardException;

    DashboardInfo findFirstDashboardInfoByTenantIdAndName(TenantId tenantId, String name);

    ListenableFuture<DashboardInfo> findFirstDashboardInfoByTenantIdAndNameAsync(TenantId tenantId, String name);

    List<Dashboard> findTenantDashboardsByTitle(TenantId tenantId, String title);

    boolean existsById(TenantId tenantId, DashboardId dashboardId);

    PageData<DashboardId> findAllDashboardsIds(PageLink pageLink);

}
