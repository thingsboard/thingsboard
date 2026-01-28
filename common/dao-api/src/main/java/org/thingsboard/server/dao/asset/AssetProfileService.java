/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface AssetProfileService extends EntityDaoService {

    AssetProfile findAssetProfileById(TenantId tenantId, AssetProfileId assetProfileId);

    AssetProfile findAssetProfileById(TenantId tenantId, AssetProfileId assetProfileId, boolean putInCache);

    AssetProfile findAssetProfileByName(TenantId tenantId, String profileName);

    AssetProfile findAssetProfileByName(TenantId tenantId, String profileName, boolean putInCache);

    AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, AssetProfileId assetProfileId);

    AssetProfile saveAssetProfile(AssetProfile assetProfile);

    AssetProfile saveAssetProfile(AssetProfile assetProfile, boolean doValidate, boolean publishSaveEvent);

    void deleteAssetProfile(TenantId tenantId, AssetProfileId assetProfileId);

    PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink);

    PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink);

    AssetProfile findOrCreateAssetProfile(TenantId tenantId, String profileName);

    AssetProfile createDefaultAssetProfile(TenantId tenantId);

    AssetProfile findDefaultAssetProfile(TenantId tenantId);

    AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId);

    boolean setDefaultAssetProfile(TenantId tenantId, AssetProfileId assetProfileId);

    void deleteAssetProfilesByTenantId(TenantId tenantId);

    List<EntityInfo> findAssetProfileNamesByTenantId(TenantId tenantId, boolean activeOnly);

    List<AssetProfileInfo> findAssetProfilesByIds(TenantId tenantId, List<AssetProfileId> assetProfileIds);

}
