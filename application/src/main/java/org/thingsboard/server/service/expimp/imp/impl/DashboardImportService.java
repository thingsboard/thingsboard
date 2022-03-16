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
package org.thingsboard.server.service.expimp.imp.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.export.impl.DashboardExportData;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.expimp.imp.EntityImportResult;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DashboardImportService extends AbstractEntityImportService<DashboardId, Dashboard, DashboardExportData> {

    private final DashboardService dashboardService;


    @Override
    public EntityImportResult<Dashboard> importEntity(TenantId tenantId, DashboardExportData exportData) {
        Dashboard dashboard = exportData.getDashboard();
        Dashboard existingDashboard = findByExternalId(tenantId, dashboard.getId());

        dashboard.setExternalId(dashboard.getId());
        dashboard.setTenantId(tenantId);

        if (existingDashboard == null) {
            dashboard.setId(null);
            dashboard.setAssignedCustomers(null); // FIXME: need to assign dashboard to customers ?
        } else {
            dashboard.setId(existingDashboard.getId());
            dashboard.setAssignedCustomers(existingDashboard.getAssignedCustomers()); // we left them untouched (FIXME)
        }

        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);

        EntityImportResult<Dashboard> importResult = new EntityImportResult<>();
        importResult.setSavedEntity(savedDashboard);
        importResult.setOldEntity(existingDashboard);
        return importResult;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
