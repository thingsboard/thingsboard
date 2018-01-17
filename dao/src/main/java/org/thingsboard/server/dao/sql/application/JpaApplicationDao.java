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
package org.thingsboard.server.dao.sql.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.application.ApplicationDao;
import org.thingsboard.server.dao.model.sql.ApplicationEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

@Component
@SqlDao
@Slf4j
public class JpaApplicationDao extends JpaAbstractSearchTextDao<ApplicationEntity, Application> implements ApplicationDao {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    protected Class<ApplicationEntity> getEntityClass() {
        return ApplicationEntity.class;
    }

    @Override
    protected CrudRepository<ApplicationEntity, String> getCrudRepository() {
        return applicationRepository;
    }


    @Override
    public List<Application> findApplicationsByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                applicationRepository.findByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }


    @Override
    public Optional<Application> findApplicationByTenantIdAndName(UUID tenantId, String name) {
        Application application = DaoUtil.getData(applicationRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name));
        return Optional.ofNullable(application);
    }

    @Override
    public List<Application> findApplicationByDeviceType(UUID tenantId, String deviceType) {
        return DaoUtil.convertDataList(applicationRepository.findByDeviceType(fromTimeUUID(tenantId), deviceType));
    }

    @Override
    public List<Application> findApplicationByRuleId(UUID tenantId, UUID ruleId) {
        return DaoUtil.convertDataList(applicationRepository.findByRuleId(fromTimeUUID(tenantId), fromTimeUUID(ruleId)));
    }

    @Override
    public List<Application> findApplicationsByDashboardId(UUID tenantId, UUID dashboardId) {
        return DaoUtil.convertDataList(applicationRepository.findByDashboardId(fromTimeUUID(tenantId), fromTimeUUID(dashboardId)));
    }


}
