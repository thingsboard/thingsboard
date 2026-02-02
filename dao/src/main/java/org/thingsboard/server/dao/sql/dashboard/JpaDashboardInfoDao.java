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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaDashboardInfoDao extends JpaAbstractDao<DashboardInfoEntity, DashboardInfo> implements DashboardInfoDao {

    @Autowired
    private DashboardInfoRepository dashboardInfoRepository;

    @Override
    protected Class<DashboardInfoEntity> getEntityClass() {
        return DashboardInfoEntity.class;
    }

    @Override
    protected JpaRepository<DashboardInfoEntity, UUID> getRepository() {
        return dashboardInfoRepository;
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantId(UUID tenantId, PageLink pageLink) {
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(new SortOrder("mobileOrder", SortOrder.Direction.ASC));
        if (pageLink.getSortOrder() != null) {
            sortOrders.add(pageLink.getSortOrder());
        }
        return DaoUtil.toPageData(dashboardInfoRepository
                .findMobileByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, sortOrders)));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(new SortOrder("mobileOrder", SortOrder.Direction.ASC));
        if (pageLink.getSortOrder() != null) {
            sortOrders.add(pageLink.getSortOrder());
        }
        return DaoUtil.toPageData(dashboardInfoRepository
                .findMobileByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, sortOrders)));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find dashboards by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public DashboardInfo findFirstByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(dashboardInfoRepository.findFirstByTenantIdAndTitle(tenantId, name));
    }

    @Override
    public String findTitleById(UUID tenantId, UUID dashboardId) {
        return dashboardInfoRepository.findTitleByTenantIdAndId(tenantId, dashboardId);
    }

    @Override
    public List<DashboardInfo> findByTenantAndImageLink(TenantId tenantId, String imageLink, int limit) {
        return DaoUtil.convertDataList(dashboardInfoRepository.findByTenantAndImageLink(tenantId.getId(), imageLink, limit));
    }

    @Override
    public List<DashboardInfo> findByImageLink(String imageLink, int limit) {
        return DaoUtil.convertDataList(dashboardInfoRepository.findByImageLink(imageLink, limit));
    }

    @Override
    public List<EntityInfo> findByTenantIdAndResource(TenantId tenantId, String reference, int limit) {
        return dashboardInfoRepository.findDashboardInfosByTenantIdAndResourceLink(tenantId.getId(), reference, PageRequest.of(0, limit));
    }

    @Override
    public List<EntityInfo> findByResource(String reference, int limit) {
        return dashboardInfoRepository.findDashboardInfosByResourceLink(reference, PageRequest.of(0, limit));
    }

}
