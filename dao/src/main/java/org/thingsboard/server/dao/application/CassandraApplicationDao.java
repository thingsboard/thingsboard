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

import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.page.TextPageLink;

import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.ApplicationEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.*;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.contains;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.*;

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
    public List<Application> findApplicationByDeviceType(UUID tenantId, String deviceType){
        log.debug("Trying to find applications by device type for tenantId [{}] and device type [{}]", tenantId, deviceType);
        Select select = select().from(APPLICATION_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME).allowFiltering();
        Select.Where query = select.where();
        query.and(eq(APPLICATION_TENANT_ID_PROPERTY, tenantId));
        query.and(contains(APPLICATION_DEVICE_TYPES_COLUMN, deviceType));
        return DaoUtil.convertDataList(findListByStatement(query));
    }

    @Override
    public List<String> findApplicationByRuleId(UUID tenantId, UUID ruleId){
        log.debug("Trying to find applications by rule id for tenantId [{}] and rule id [{}]", tenantId, ruleId);
        Select select = select().from(APPLICATION_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME).allowFiltering();
        Select.Where query = select.where();
        query.and(eq(APPLICATION_TENANT_ID_PROPERTY, tenantId));
        query.and(contains(APPLICATION_RULES_COLUMN, ruleId));
        return findListByStatement(query).stream().map(ApplicationEntity::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> findApplicationsByDashboardId(UUID tenantId, UUID dashboardId) {
        log.debug("Trying to find applications by dashboard id for tenantId [{}] and dashboard id [{}]", tenantId, dashboardId);

        Select.Where dashBoardQuery =select().from(APPLICATION_BY_TENANT_AND_DASHBOARD_COLUMN_FAMILY).where();
        dashBoardQuery.and(eq(APPLICATION_TENANT_ID_PROPERTY, tenantId));
        dashBoardQuery.and(eq(APPLICATION_DASHBOARD_ID_PROPERTY, dashboardId));
        List<String> dashboardApplications =  findListByStatement(dashBoardQuery).stream().map(ApplicationEntity::getName).collect(Collectors.toList());

        Select.Where miniDashBoardQuery = select().from(APPLICATION_BY_TENANT_AND_MINI_DASHBOARD_COLUMN_FAMILY).where();
        miniDashBoardQuery.and(eq(APPLICATION_TENANT_ID_PROPERTY, tenantId));
        miniDashBoardQuery.and(eq(APPLICATION_MINI_DASHBOARD_ID_PROPERTY, dashboardId));
        List<String> miniDashboardApplications =  findListByStatement(miniDashBoardQuery).stream().map(ApplicationEntity::getName).collect(Collectors.toList());

        Set<String> combined = new LinkedHashSet<>(dashboardApplications);
        combined.addAll(miniDashboardApplications);
        return new ArrayList<>(combined);
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
    public Optional<Application> findApplicationByTenantIdAndName(UUID tenantId, String applicationName) {
        Select select = select().from(APPLICATION_BY_TENANT_AND_NAME_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(APPLICATION_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(APPLICATION_NAME, applicationName));
        return Optional.ofNullable(DaoUtil.getData(findOneByStatement(query)));
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
