/**
 * Copyright © 2016 The Thingsboard Authors
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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractSearchTextDao;
import org.thingsboard.server.dao.model.DashboardEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Slf4j
public class DashboardDaoImpl extends AbstractSearchTextDao<DashboardEntity> implements DashboardDao {

    @Override
    protected Class<DashboardEntity> getColumnFamilyClass() {
        return DashboardEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return DASHBOARD_COLUMN_FAMILY_NAME;
    }

    @Override
    public DashboardEntity save(Dashboard dashboard) {
        log.debug("Save dashboard [{}] ", dashboard);
        return save(new DashboardEntity(dashboard));
    }

    @Override
    public List<DashboardEntity> findDashboardsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find dashboards by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<DashboardEntity> dashboardEntities = findPageWithTextSearch(DASHBOARD_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME, 
                Arrays.asList(eq(DASHBOARD_TENANT_ID_PROPERTY, tenantId),
                              eq(DASHBOARD_CUSTOMER_ID_PROPERTY, NULL_UUID)), 
                pageLink); 
        
        log.trace("Found dashboards [{}] by tenantId [{}] and pageLink [{}]", dashboardEntities, tenantId, pageLink);
        return dashboardEntities;
    }

    @Override
    public List<DashboardEntity> findDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        log.debug("Try to find dashboards by tenantId [{}], customerId[{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<DashboardEntity> dashboardEntities = findPageWithTextSearch(DASHBOARD_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME, 
                Arrays.asList(eq(DASHBOARD_CUSTOMER_ID_PROPERTY, customerId),
                              eq(DASHBOARD_TENANT_ID_PROPERTY, tenantId)), 
                pageLink); 
        
        log.trace("Found dashboards [{}] by tenantId [{}], customerId [{}] and pageLink [{}]", dashboardEntities, tenantId, customerId, pageLink);
        return dashboardEntities;
    }

}
