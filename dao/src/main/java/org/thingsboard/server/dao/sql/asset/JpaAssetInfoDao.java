// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.asset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetInfoDao;
import org.thingsboard.server.dao.model.sql.AssetInfoEntity;
import org.thingsboard.server.dao.sql.CitusSubCustomerInfoQuery;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaAssetInfoDao extends JpaAbstractDao<AssetInfoEntity, AssetInfo> implements AssetInfoDao {

    @Autowired
    private AssetInfoRepository assetInfoRepository;

    @Autowired
    private CitusSubCustomerInfoQuery citusSubCustomerInfoQuery;

    @Override
    protected Class<AssetInfoEntity> getEntityClass() {
        return AssetInfoEntity.class;
    }

    @Override
    protected JpaRepository<AssetInfoEntity, UUID> getRepository() {
        return assetInfoRepository;
    }

    @Override
    public PageData<AssetInfo> findAssetsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(assetInfoRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetInfo> findAssetsByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, PageLink pageLink) {
        return DaoUtil.toPageData(assetInfoRepository
                .findByTenantIdAndAssetProfileId(
                        tenantId,
                        assetProfileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetInfo> findTenantAssetsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(assetInfoRepository
                .findTenantAssetsByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetInfo> findTenantAssetsByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, PageLink pageLink) {
        return DaoUtil.toPageData(assetInfoRepository
                .findTenantAssetsByTenantIdAndAssetProfileId(
                        tenantId,
                        assetProfileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetInfo> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(assetInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetInfo> findAssetsByTenantIdAndCustomerIdAndAssetProfileId(UUID tenantId, UUID customerId, UUID assetProfileId, PageLink pageLink) {
        return DaoUtil.toPageData(assetInfoRepository
                .findByTenantIdAndCustomerIdAndAssetProfileId(
                        tenantId,
                        customerId,
                        assetProfileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetInfo> findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return citusSubCustomerInfoQuery.findIncludingSubCustomers(
                tenantId, customerId,
                customerIds -> assetInfoRepository.findByTenantIdAndCustomerIdInCustomerIds(
                        tenantId, customerId, customerIds, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)),
                () -> assetInfoRepository.findByTenantIdAndCustomerIdIncludingSubCustomers(
                        tenantId, customerId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetInfo> findAssetsByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(UUID tenantId, UUID customerId, UUID assetProfileId, PageLink pageLink) {
        return citusSubCustomerInfoQuery.findIncludingSubCustomers(
                tenantId, customerId,
                customerIds -> assetInfoRepository.findByTenantIdAndCustomerIdAndAssetProfileIdInCustomerIds(
                        tenantId, customerId, customerIds, assetProfileId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)),
                () -> assetInfoRepository.findByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(
                        tenantId, customerId, assetProfileId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }
}
