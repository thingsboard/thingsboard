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
package org.thingsboard.server.dao.service.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;

@Component
public class DashboardDataValidator extends DataValidator<Dashboard> {

    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, Dashboard data) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxDashboards = profileConfiguration.getMaxDashboards();
        validateNumberOfEntitiesPerTenant(tenantId, dashboardDao, maxDashboards, EntityType.DASHBOARD);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Dashboard dashboard) {
        if (StringUtils.isEmpty(dashboard.getTitle())) {
            throw new DataValidationException("Dashboard title should be specified!");
        }
        if (dashboard.getTenantId() == null) {
            throw new DataValidationException("Dashboard should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(dashboard.getTenantId())) {
                throw new DataValidationException("Dashboard is referencing to non-existent tenant!");
            }
        }
    }
}
