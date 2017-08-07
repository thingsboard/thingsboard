/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface AssetDao.
 *
 */
public interface AssetDao extends Dao<Asset> {

    /**
     * Save or update asset object
     *
     * @param asset the asset object
     * @return saved asset object
     */
    Asset save(Asset asset);

    /**
     * Find assets by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of asset objects
     */
    List<Asset> findAssetsByTenantId(UUID tenantId, TextPageLink pageLink);

    /**
     * Find assets by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset objects
     */
    List<Asset> findAssetsByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink);

    /**
     * Find assets by tenantId and assets Ids.
     *
     * @param tenantId the tenantId
     * @param assetIds the asset Ids
     * @return the list of asset objects
     */
    ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds);

    /**
     * Find assets by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of asset objects
     */
    List<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink);

    /**
     * Find assets by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of asset objects
     */
    List<Asset> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink);

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
    ListenableFuture<List<EntitySubtype>> findTenantAssetTypesAsync(UUID tenantId);

}
