// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.asset;

import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface AssetInfoDao extends Dao<AssetInfo> {

    PageData<AssetInfo> findAssetsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<AssetInfo> findAssetsByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, PageLink pageLink);

    PageData<AssetInfo> findTenantAssetsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<AssetInfo> findTenantAssetsByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, PageLink pageLink);

    PageData<AssetInfo> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<AssetInfo> findAssetsByTenantIdAndCustomerIdAndAssetProfileId(UUID tenantId, UUID customerId, UUID assetProfileId, PageLink pageLink);

    PageData<AssetInfo> findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<AssetInfo> findAssetsByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(UUID tenantId, UUID customerId, UUID assetProfileId, PageLink pageLink);
}
