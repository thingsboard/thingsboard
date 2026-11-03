// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetProfileImportService extends BaseEntityImportService<AssetProfileId, AssetProfile, EntityExportData<AssetProfile>> {

    private final AssetProfileService assetProfileService;

    @Override
    protected void setOwner(TenantId tenantId, AssetProfile assetProfile, IdProvider idProvider) {
        assetProfile.setTenantId(tenantId);
    }

    @Override
    protected AssetProfile prepare(EntitiesImportCtx ctx, AssetProfile assetProfile, AssetProfile old, EntityExportData<AssetProfile> exportData, IdProvider idProvider) {
        assetProfile.setDefaultRuleChainId(idProvider.getInternalId(assetProfile.getDefaultRuleChainId()));
        assetProfile.setDefaultDashboardId(idProvider.getInternalId(assetProfile.getDefaultDashboardId()));
        assetProfile.setDefaultEdgeRuleChainId(idProvider.getInternalId(assetProfile.getDefaultEdgeRuleChainId()));
        return assetProfile;
    }

    @Override
    protected AssetProfile saveOrUpdate(EntitiesImportCtx ctx, AssetProfile assetProfile, EntityExportData<AssetProfile> exportData, IdProvider idProvider, CompareResult compareResult) {
        AssetProfile saved = assetProfileService.saveAssetProfile(assetProfile);
        if (ctx.isFinalImportAttempt() || ctx.getCurrentImportResult().isUpdatedAllExternalIds()) {
            importCalculatedFields(ctx, saved, exportData, idProvider);
        }
        return saved;
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
