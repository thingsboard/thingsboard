/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.DashboardFields;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.model.sql.DashboardEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaDashboardDao extends JpaAbstractDao<DashboardEntity, Dashboard> implements DashboardDao {

    @Autowired
    DashboardRepository dashboardRepository;

    @Override
    protected Class<DashboardEntity> getEntityClass() {
        return DashboardEntity.class;
    }

    @Override
    protected JpaRepository<DashboardEntity, UUID> getRepository() {
        return dashboardRepository;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return dashboardRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public Dashboard findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(dashboardRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<Dashboard> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public DashboardId getExternalIdByInternal(DashboardId internalId) {
        return Optional.ofNullable(dashboardRepository.getExternalIdById(internalId.getId()))
                .map(DashboardId::new).orElse(null);
    }

    @Override
    public List<Dashboard> findByTenantIdAndTitle(UUID tenantId, String title) {
        return DaoUtil.convertDataList(dashboardRepository.findByTenantIdAndTitle(tenantId, title));
    }

    @Override
    public PageData<DashboardId> findIdsByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(dashboardRepository.findIdsByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)).map(DashboardId::new));
    }

    @Override
    public PageData<DashboardId> findAllIds(PageLink pageLink) {
        return DaoUtil.pageToPageData(dashboardRepository.findAllIds(DaoUtil.toPageable(pageLink)).map(DashboardId::new));
    }

    @Override
    public PageData<Dashboard> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<DashboardFields> findNextBatch(UUID id, int batchSize) {
        return dashboardRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
