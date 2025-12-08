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

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.ImageContainerDao;

import java.util.List;
import java.util.UUID;

public interface AssetProfileDao extends Dao<AssetProfile>, ExportableEntityDao<AssetProfileId, AssetProfile>, ImageContainerDao<AssetProfileInfo> {

    AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, UUID assetProfileId);

    AssetProfile save(TenantId tenantId, AssetProfile assetProfile);

    AssetProfile saveAndFlush(TenantId tenantId, AssetProfile assetProfile);

    PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink);

    PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink);

    AssetProfile findDefaultAssetProfile(TenantId tenantId);

    AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId);

    AssetProfile findByName(TenantId tenantId, String profileName);

    PageData<AssetProfile> findAllWithImages(PageLink pageLink);

    List<EntityInfo> findTenantAssetProfileNames(UUID tenantId, boolean activeOnly);

    List<AssetProfileInfo> findAssetProfilesByTenantIdAndIds(UUID tenantId, List<UUID> assetProfileIds);

}
