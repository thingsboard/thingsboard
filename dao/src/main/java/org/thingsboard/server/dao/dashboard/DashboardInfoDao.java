// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.dashboard;

import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ImageContainerDao;
import org.thingsboard.server.dao.ResourceContainerDao;

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
     * Find dashboards by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param pageLink the page link
     * @return the list of dashboard objects
     */
    PageData<DashboardInfo> findDashboardsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    DashboardInfo findFirstByTenantIdAndName(UUID tenantId, String name);

    String findTitleById(UUID tenantId, UUID dashboardId);

}
