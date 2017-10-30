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
package org.thingsboard.server.dao.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.page.TextPageLink;

import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.ApplicationEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.thingsboard.server.dao.model.ModelConstants.APPLICATION_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.APPLICATION_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.APPLICATION_TENANT_ID_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraApplicationDao extends CassandraAbstractSearchTextDao<ApplicationEntity, Application> implements ApplicationDao{

    @Override
    public Application save(Application application){
        Application savedApplication = super.save(application);
        return savedApplication;
    }


    @Override
    public List<Application> findApplicationsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Trying to find applications by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<ApplicationEntity> applicationEntities = findPageWithTextSearch(APPLICATION_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(APPLICATION_TENANT_ID_PROPERTY, tenantId)), pageLink);
        log.trace("Found applications [{}] by tenantId [{}] and pageLink [{}]", applicationEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(applicationEntities);
    }



    @Override
    protected Class<ApplicationEntity> getColumnFamilyClass() {
        return ApplicationEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return APPLICATION_TABLE_NAME;
    }
}
