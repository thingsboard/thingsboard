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

    ListenableFuture<List<DashboardInfo>> findDashboardsByIdsAsync(UUID tenantId, List<UUID> dashboardIds);

}
