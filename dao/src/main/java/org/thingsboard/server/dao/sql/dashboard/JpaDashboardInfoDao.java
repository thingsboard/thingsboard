// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
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
    public DashboardInfo findById(TenantId tenantId, UUID dashboardId) {
        log.debug("Get entity by key {}", dashboardId);
        return DaoUtil.getData(dashboardInfoRepository.findFullInfoById(dashboardId));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
    }

    @Override
    public PageData<DashboardInfo> findTenantDashboardsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findTenantDashboardsByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
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
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap, sortOrders)));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantIdAndCustomerIdIncludingSubCustomers(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
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
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap, sortOrders)));
    }

    @Override
    public ListenableFuture<List<DashboardInfo>> findDashboardsByIdsAsync(UUID tenantId, List<UUID> dashboardIds) {
        return service.submit(() -> DaoUtil.convertDataList(dashboardInfoRepository.findByIdIn(dashboardIds)));
    }

    @Override
    public List<DashboardInfo> findDashboardsByIds(UUID tenantId, List<UUID> dashboardIds) {
        return DaoUtil.convertDataList(dashboardInfoRepository.findByIdIn(dashboardIds));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByEntityGroupId(
                        groupId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByEntityGroupIds(
                        groupIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(new SortOrder("mobileOrder", SortOrder.Direction.ASC));
        if (pageLink.getSortOrder() != null) {
            sortOrders.add(pageLink.getSortOrder());
        }
        return DaoUtil.toPageData(dashboardInfoRepository
                .findMobileByEntityGroupIds(
                        groupIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap, sortOrders)));
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
