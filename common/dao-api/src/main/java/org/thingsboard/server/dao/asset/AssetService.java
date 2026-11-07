// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface AssetService extends EntityDaoService {

    Asset findAssetById(TenantId tenantId, AssetId assetId);

    AssetInfo findAssetInfoById(TenantId tenantId, AssetId assetId);

    EntityInfo findAssetEntityInfoById(TenantId tenantId, AssetId assetId);

    ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, AssetId assetId);

    Asset findAssetByTenantIdAndName(TenantId tenantId, String name);

    ListenableFuture<Asset> findAssetByTenantIdAndNameAsync(TenantId tenantId, String name);

    Asset saveAsset(Asset asset, boolean doValidate);

    Asset saveAsset(Asset asset);

    Asset saveAsset(Asset asset, NameConflictStrategy nameConflictStrategy);

    void deleteAsset(TenantId tenantId, AssetId assetId);

    PageData<Asset> findAssetsByTenantId(TenantId tenantId, PageLink pageLink);

    Long countAssets();

    PageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    /**
     * @deprecated reads one page per call with OFFSET and a full count, so iterating all pages costs O(N²/pageSize) rows;
     * use {@link #findProfileEntityIdInfosByTenantId(TenantId, int, Consumer)} instead. Scheduled for removal in the next major release.
     */
    @Deprecated(since = "4.4", forRemoval = true)
    PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink);

    /**
     * @deprecated reads one page per call with OFFSET and a full count, so iterating all pages costs O(N²/pageSize) rows;
     * use {@link #findProfileEntityIdInfosByTenantId(TenantId, int, Consumer)} instead. Scheduled for removal in the next major release.
     */
    @Deprecated(since = "4.4", forRemoval = true)
    PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * Passes all assets of the tenant to {@code pageConsumer} page by page: keyset pages of up to {@code pageSize} rows over the
     * {@code (tenant_id, id)} index, with no OFFSET and no count. A page is handed over after its query has finished, so no
     * database connection or transaction is held while the consumer runs.
     */
    void findProfileEntityIdInfosByTenantId(TenantId tenantId, int pageSize, Consumer<List<ProfileEntityIdInfo>> pageConsumer);

    PageData<AssetId> findAssetIdsByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink);

    PageData<EntityInfo> findAssetEntityInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink);

    ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds);

    void deleteAssetsByTenantId(TenantId tenantId);

    PageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<AssetId> findAssetIdsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    void deleteAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<AssetId> assetIds);

    ListenableFuture<List<Asset>> findAssetsByQuery(TenantId tenantId, AssetSearchQuery query);

    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(TenantId tenantId);

    PageData<Asset> findAssetsByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<Asset> findAssetsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<Asset> findAssetsByEntityGroupIdsAndType(List<EntityGroupId> groupIds, String type, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink);

    PageData<AssetInfo> findTenantAssetInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<AssetInfo> findTenantAssetInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, PageLink pageLink);

    List<AssetCacheInfo> findAssetCacheInfosByRelationType(String relationType, UUID id, int batchSize);

}
