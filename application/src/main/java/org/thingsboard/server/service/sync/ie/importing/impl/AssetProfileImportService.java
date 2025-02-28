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
package org.thingsboard.server.service.sync.ie.importing.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.AssetProfileExportData;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
public class AssetProfileImportService extends BaseCalculatedFieldsImportService<AssetProfileId, AssetProfile, AssetProfileExportData> {

    private final AssetProfileService assetProfileService;

    public AssetProfileImportService(CalculatedFieldService calculatedFieldService, AssetProfileService assetProfileService) {
        super(calculatedFieldService);
        this.assetProfileService = assetProfileService;
    }

    @Override
    protected void setOwner(TenantId tenantId, AssetProfile assetProfile, IdProvider idProvider) {
        assetProfile.setTenantId(tenantId);
    }

    @Override
    protected AssetProfile prepare(EntitiesImportCtx ctx, AssetProfile assetProfile, AssetProfile old, AssetProfileExportData exportData, IdProvider idProvider) {
        assetProfile.setDefaultRuleChainId(idProvider.getInternalId(assetProfile.getDefaultRuleChainId()));
        assetProfile.setDefaultDashboardId(idProvider.getInternalId(assetProfile.getDefaultDashboardId()));
        assetProfile.setDefaultEdgeRuleChainId(idProvider.getInternalId(assetProfile.getDefaultEdgeRuleChainId()));
        return assetProfile;
    }

    @Override
    protected AssetProfile saveOrUpdate(EntitiesImportCtx ctx, AssetProfile assetProfile, AssetProfileExportData exportData, IdProvider idProvider) {
        return saveOrUpdateEntity(ctx, assetProfile, exportData, idProvider, assetProfileService::saveAssetProfile);
    }

    @Override
    protected void onEntitySaved(User user, AssetProfile savedAssetProfile, AssetProfile oldAssetProfile) {
        logEntityActionService.logEntityAction(savedAssetProfile.getTenantId(), savedAssetProfile.getId(),
                savedAssetProfile, null, oldAssetProfile == null ? ActionType.ADDED : ActionType.UPDATED, user);
    }

    @Override
    protected AssetProfile deepCopy(AssetProfile assetProfile) {
        return new AssetProfile(assetProfile);
    }

    @Override
    protected void cleanupForComparison(AssetProfile assetProfile) {
        super.cleanupForComparison(assetProfile);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }

}
