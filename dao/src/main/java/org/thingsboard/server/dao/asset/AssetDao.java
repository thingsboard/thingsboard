// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableCustomerEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface AssetDao.
 *
 */
public interface AssetDao extends Dao<Asset>, TenantEntityDao<Asset>, ExportableCustomerEntityDao<Asset, AssetId> {

    /**
     * Save or update asset object
     *
     * @param asset the asset object
     * @return saved asset object
     */
    Asset save(TenantId tenantId, Asset asset);

    /**
     * Find assets by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of asset objects
     */
    PageData<Asset> findAssetsByTenantId(UUID tenantId, PageLink pageLink);

    Long countAssets();

    /**
     * Find assets by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset objects
     */
    PageData<Asset> findAssetsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find asset ids by tenantId, assetProfileId and page link.
     *
     * @param tenantId the tenantId
     * @param assetProfileId the assetProfileId
     * @param pageLink the page link
     * @return the list of asset objects
     */
    PageData<AssetId> findAssetIdsByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, PageLink pageLink);

    EntityInfo findAssetEntityInfoById(TenantId tenantId, AssetId assetId);

    PageData<EntityInfo> findAssetEntityInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink);

    /**
     * Find assets by tenantId and assets Ids.
     *
     * @param tenantId the tenantId
     * @param assetIds the asset Ids
     * @return the list of asset objects
     */
    ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds);

    PageData<Asset> findAssetsByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<Asset> findAssetsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    PageData<Asset> findAssetsByEntityGroupIdsAndType(List<UUID> groupIds, String type, PageLink pageLink);

    /**
     * Find assets by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of asset objects
     */
    PageData<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find assets by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset objects
     */
    PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * Find assets by tenantId, customerId and assets Ids.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param assetIds the asset Ids
     * @return the list of asset objects
     */
    ListenableFuture<List<Asset>> findAssetsByTenantIdAndCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds);

    /**
     * Find assets by tenantId and asset name.
     *
     * @param tenantId the tenantId
     * @param name the asset name
     * @return the optional asset object
     */
    Optional<Asset> findAssetsByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find tenants asset types.
     *
     * @return the list of tenant asset type objects
     */
    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findTenantAssetTypesAsync(UUID tenantId);

    Long countAssetsByAssetProfileId(TenantId tenantId, UUID assetProfileId);

    /**
     * Find assets by tenantId, profileId and page link.
     *
     * @param tenantId the tenantId
     * @param profileId the profileId
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Asset> findAssetsByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink);

    PageData<TbPair<UUID, String>> getAllAssetTypes(PageLink pageLink);

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink);

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, PageLink pageLink);

    List<AssetCacheInfo> findAssetCacheInfosByRelationType(String relationType, UUID id, int batchSize);

}
