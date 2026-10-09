// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
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
@Slf4j
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
    public Long countDashboards() {
        return dashboardRepository.count();
    }

    @Override
    public PageData<DashboardId> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> page;
        if (customerId == null) {
            page = dashboardRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        } else {
            page = dashboardRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(page, DashboardId::new);
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
    public Long countScadaDashboards() {
        return dashboardRepository.countAllDashboardsByLayoutType("scada");
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

    @Override
    public void replacePatternInAllDashboardsConfigurations(String pattern, String replacement) {
        int rowCount = dashboardRepository.replaceStringInAllDashboardConfigs(pattern, replacement);
        log.trace("replacePatternInAllDashboardsConfigurations: Affected row count: {}", rowCount);
    }

    @Override
    public void replaceWidgetTypeFullFqn(String oldLink, String newLink) {
        int rowCount = dashboardRepository.replaceWidgetTypeFullFqn(oldLink, newLink);
        log.trace("replaceWidgetTypeFullFqn: Affected row count: {}", rowCount);
    }

    @Override
    public void setTrendzWidgetsTypeLatestBySystemFqn(String systemFqn) {
        int rowCount = dashboardRepository.setTrendzWidgetsTypeLatestBySystemFqn(systemFqn);
        log.trace("setTrendzWidgetsTypeLatestBySystemFqn: Affected row count: {}", rowCount);
    }

}
