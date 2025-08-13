/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface AssetService extends EntityDaoService {

    AssetInfo findAssetInfoById(TenantId tenantId, AssetId assetId);

    Asset findAssetById(TenantId tenantId, AssetId assetId);

    ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, AssetId assetId);

    Asset findAssetByTenantIdAndName(TenantId tenantId, String name);

    ListenableFuture<Asset> findAssetByTenantIdAndNameAsync(TenantId tenantId, String name);

    Asset saveAsset(Asset asset, boolean doValidate);

    Asset saveAsset(Asset asset);

    Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, CustomerId customerId);

    Asset unassignAssetFromCustomer(TenantId tenantId, AssetId assetId);

    void deleteAsset(TenantId tenantId, AssetId assetId);

    PageData<Asset> findAssetsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink);

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink);

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<AssetId> findAssetIdsByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink);

    ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds);

    void deleteAssetsByTenantId(TenantId tenantId);

    PageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, PageLink pageLink);

    ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<AssetId> assetIds);

    void unassignCustomerAssets(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<Asset>> findAssetsByQuery(TenantId tenantId, AssetSearchQuery query);

    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(TenantId tenantId);

    Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, EdgeId edgeId);

    Asset unassignAssetFromEdge(TenantId tenantId, AssetId assetId, EdgeId edgeId);

    PageData<Asset> findAssetsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink);
}
