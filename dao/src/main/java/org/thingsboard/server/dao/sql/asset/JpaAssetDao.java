// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.asset;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edqs.fields.AssetFields;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.device.NativeAssetRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.convertTenantEntityInfosToDto;

@Component
@SqlDao
@Slf4j
public class JpaAssetDao extends JpaAbstractDao<AssetEntity, Asset> implements AssetDao {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private NativeAssetRepository nativeAssetRepository;

    @Autowired
    private AssetProfileRepository assetProfileRepository;

    @Override
    protected Class<AssetEntity> getEntityClass() {
        return AssetEntity.class;
    }

    @Override
    protected JpaRepository<AssetEntity, UUID> getRepository() {
        return assetRepository;
    }

    @Override
    public PageData<Asset> findAssetsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Long countAssets() {
        return assetRepository.count();
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds) {
        return DaoUtil.getEntitiesByTenantIdAndIdIn(assetIds, ids ->
                assetRepository.findByTenantIdAndIdIn(tenantId, ids), service);
    }

    @Override
    public PageData<Asset> findAssetsByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByEntityGroupId(
                        groupId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Asset> findAssetsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByEntityGroupIds(
                        groupIds,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Asset> findAssetsByEntityGroupIdsAndType(List<UUID> groupIds, String type, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByEntityGroupIdsAndType(
                        groupIds,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds) {
        return DaoUtil.getEntitiesByTenantIdAndIdIn(assetIds, ids ->
                assetRepository.findByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, ids), service);
    }

    @Override
    public Optional<Asset> findAssetsByTenantIdAndName(UUID tenantId, String name) {
        Asset asset = DaoUtil.getData(assetRepository.findFirstByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(asset);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetId> findAssetIdsByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, PageLink pageLink) {
        return DaoUtil.pageToPageData(assetRepository.findAssetIdsByTenantIdAndAssetProfileId(
                        tenantId,
                        assetProfileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)))
                .mapData(AssetId::new);
    }

    @Override
    public PageData<EntityInfo> findAssetEntityInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink) {
        return DaoUtil.pageToPageData(assetRepository.findEntityInfosByTenantIdAndProfileId(
                tenantId.getId(),
                assetProfileId.getId(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetId> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> page;
        if (customerId == null) {
            page = assetRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        } else {
            page = assetRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(page, AssetId::new);
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantAssetTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityInfosToDto(tenantId, EntityType.ASSET, assetProfileRepository.findActiveTenantAssetProfileNames(tenantId)));
    }

    @Override
    public Long countAssetsByAssetProfileId(TenantId tenantId, UUID assetProfileId) {
        return assetRepository.countByAssetProfileId(assetProfileId);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findByTenantIdAndProfileId(
                        tenantId,
                        profileId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<TbPair<UUID, String>> getAllAssetTypes(PageLink pageLink) {
        log.debug("Try to find all asset types and pageLink [{}]", pageLink);
        return DaoUtil.pageToPageData(assetRepository.getAllAssetTypes(
                DaoUtil.toPageable(pageLink, Arrays.asList(new SortOrder("tenantId"), new SortOrder("type")))));
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink) {
        log.debug("Find profile asset id infos by pageLink [{}]", pageLink);
        return nativeAssetRepository.findProfileEntityIdInfos(DaoUtil.toPageable(pageLink));
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, PageLink pageLink) {
        log.debug("Find profile asset id infos by pageLink [{}]", pageLink);
        return nativeAssetRepository.findProfileEntityIdInfosByTenantId(tenantId, DaoUtil.toPageable(pageLink));
    }

    @Override
    public EntityInfo findAssetEntityInfoById(TenantId tenantId, AssetId assetId) {
        return assetRepository.findEntityInfoById(assetId.getId());
    }

    @Override
    public List<EntityInfo> findEntityInfosByNamePrefix(TenantId tenantId, String name) {
        log.debug("Find asset entity infos by name [{}]", name);
        return assetRepository.findEntityInfosByNamePrefix(tenantId.getId(), name);
    }

    @Override
    public List<AssetCacheInfo> findAssetCacheInfosByRelationType(String relationType, UUID id, int batchSize) {
        return assetRepository.findAssetCacheInfosByRelationType(relationType, id, Limit.of(batchSize));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return assetRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public Asset findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(assetRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Asset findByTenantIdAndName(UUID tenantId, String name) {
        return findAssetsByTenantIdAndName(tenantId, name).orElse(null);
    }

    @Override
    public PageData<Asset> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAssetsByTenantId(tenantId, pageLink);
    }

    @Override
    public AssetId getExternalIdByInternal(AssetId internalId) {
        return Optional.ofNullable(assetRepository.getExternalIdById(internalId.getId()))
                .map(AssetId::new).orElse(null);
    }

    @Override
    public PageData<Asset> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<AssetFields> findNextBatch(UUID uuid, int batchSize) {
        return assetRepository.findAllFields(uuid, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
