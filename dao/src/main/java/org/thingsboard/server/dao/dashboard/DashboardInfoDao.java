// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ImageContainerDao;
import org.thingsboard.server.dao.ResourceContainerDao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface DashboardInfoDao.
 */
public interface DashboardInfoDao extends Dao<DashboardInfo>, ImageContainerDao<DashboardInfo>, ResourceContainerDao<DashboardInfo> {

    /**
     * Find dashboards by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of dashboard objects
     */
    PageData<DashboardInfo> findDashboardsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<DashboardInfo> findTenantDashboardsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find dashboards not hidden for mobile by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of dashboard objects
     */
    PageData<DashboardInfo> findMobileDashboardsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find dashboards by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of dashboard objects
     */
    PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find dashboards not hidden for mobile by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of dashboard objects
     */
    PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find dashboards by dashboard Ids.
     *
     * @param tenantId the tenantId
     * @param dashboardIds the dashboard Ids
     * @return the list of dashboard objects
     */
    ListenableFuture<List<DashboardInfo>> findDashboardsByIdsAsync(UUID tenantId, List<UUID> dashboardIds);

    List<DashboardInfo> findDashboardsByIds(UUID tenantId, List<UUID> dashboardIds);

    PageData<DashboardInfo> findDashboardsByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<DashboardInfo> findDashboardsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    PageData<DashboardInfo> findMobileDashboardsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    DashboardInfo findFirstByTenantIdAndName(UUID tenantId, String name);

    String findTitleById(UUID tenantId, UUID dashboardId);

}
