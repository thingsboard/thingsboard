// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.entityview;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entityview.EntityViewInfoDao;
import org.thingsboard.server.dao.model.sql.EntityViewInfoEntity;
import org.thingsboard.server.dao.sql.CitusSubCustomerInfoQuery;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaEntityViewInfoDao extends JpaAbstractDao<EntityViewInfoEntity, EntityViewInfo> implements EntityViewInfoDao {

    @Autowired
    private EntityViewInfoRepository entityViewInfoRepository;

    @Autowired
    private CitusSubCustomerInfoQuery citusSubCustomerInfoQuery;

    @Override
    protected Class<EntityViewInfoEntity> getEntityClass() {
        return EntityViewInfoEntity.class;
    }

    @Override
    protected JpaRepository<EntityViewInfoEntity, UUID> getRepository() {
        return entityViewInfoRepository;
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewInfoRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewInfoRepository
                .findByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findTenantEntityViewsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewInfoRepository
                .findTenantEntityViewsByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findTenantEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewInfoRepository
                .findTenantEntityViewsByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewInfoRepository
                .findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return citusSubCustomerInfoQuery.findIncludingSubCustomers(
                tenantId, customerId,
                customerIds -> entityViewInfoRepository.findByTenantIdAndCustomerIdInCustomerIds(
                        tenantId, customerId, customerIds, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)),
                () -> entityViewInfoRepository.findByTenantIdAndCustomerIdIncludingSubCustomers(
                        tenantId, customerId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewsByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return citusSubCustomerInfoQuery.findIncludingSubCustomers(
                tenantId, customerId,
                customerIds -> entityViewInfoRepository.findByTenantIdAndCustomerIdAndTypeInCustomerIds(
                        tenantId, customerId, customerIds, type, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)),
                () -> entityViewInfoRepository.findByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(
                        tenantId, customerId, type, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }
}
