// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.dashboard;

import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableCustomerEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface DashboardDao.
 */
public interface DashboardDao extends Dao<Dashboard>, TenantEntityDao<Dashboard>, ExportableCustomerEntityDao<Dashboard, DashboardId> {

    /**
     * Save or update dashboard object
     *
     * @param dashboard the dashboard object
     * @return saved dashboard object
     */
    Dashboard save(TenantId tenantId, Dashboard dashboard);

    List<Dashboard> findByTenantIdAndTitle(UUID tenantId, String title);

    PageData<DashboardId> findIdsByTenantId(TenantId tenantId, PageLink pageLink);

    Long countDashboards();

    PageData<DashboardId> findAllIds(PageLink pageLink);

    void replacePatternInAllDashboardsConfigurations(String pattern, String replacement);

    Long countScadaDashboards();

    void replaceWidgetTypeFullFqn(String oldLink, String newLink);

    void setTrendzWidgetsTypeLatestBySystemFqn(String systemFqn);

}
