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
import org.thingsboard.server.dao.model.DashboardInfoEntity;

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

}
