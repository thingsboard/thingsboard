// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class AssetProfileExportService extends BaseEntityExportService<AssetProfileId, AssetProfile, EntityExportData<AssetProfile>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, AssetProfile assetProfile, EntityExportData<AssetProfile> exportData) {
        assetProfile.setDefaultDashboardId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultDashboardId()));
        assetProfile.setDefaultRuleChainId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultRuleChainId()));
        assetProfile.setDefaultEdgeRuleChainId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultEdgeRuleChainId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ASSET_PROFILE);
    }

}
