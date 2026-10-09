// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.owner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OwnerInfoEntity;
import org.thingsboard.server.dao.owner.OwnerInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
public class JpaOwnerInfoDao extends JpaAbstractDao<OwnerInfoEntity, EntityInfo> implements OwnerInfoDao {

    @Autowired
    private OwnerInfoRepository ownerInfoRepository;

    @Override
    protected Class<OwnerInfoEntity> getEntityClass() {
        return OwnerInfoEntity.class;
    }

    @Override
    protected JpaRepository<OwnerInfoEntity, UUID> getRepository() {
        return ownerInfoRepository;
    }

    @Override
    public PageData<EntityInfo> findTenantOwnerByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(ownerInfoRepository
                .findTenantOwnerByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findCustomerOwnersByTenantIdIncludingTenant(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(ownerInfoRepository
                .findCustomerOwnersByTenantIdIncludingTenant(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findCustomerOwnersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(ownerInfoRepository
                .findCustomerOwnersByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findCustomerOwnersByIdsAndTenantId(UUID tenantId, List<UUID> ownerIds, PageLink pageLink) {
        return DaoUtil.toPageData(ownerInfoRepository
                .findCustomerOwnersByIdsAndTenantId(
                        tenantId,
                        ownerIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }
}
