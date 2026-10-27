// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.edge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeInfoDao;
import org.thingsboard.server.dao.model.sql.EdgeInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaEdgeInfoDao extends JpaAbstractDao<EdgeInfoEntity, EdgeInfo> implements EdgeInfoDao {

    @Autowired
    private EdgeInfoRepository EdgeInfoRepository;

    @Override
    protected Class<EdgeInfoEntity> getEntityClass() {
        return EdgeInfoEntity.class;
    }

    @Override
    protected JpaRepository<EdgeInfoEntity, UUID> getRepository() {
        return EdgeInfoRepository;
    }

    @Override
    public PageData<EdgeInfo> findEdgesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(EdgeInfoRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(EdgeInfoRepository
                .findByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findTenantEdgesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(EdgeInfoRepository
                .findTenantEdgesByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findTenantEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(EdgeInfoRepository
                .findTenantEdgesByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(EdgeInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(EdgeInfoRepository
                .findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findEdgesByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(EdgeInfoRepository
                .findByTenantIdAndCustomerIdIncludingSubCustomers(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findEdgesByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(EdgeInfoRepository
                .findByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }
}
