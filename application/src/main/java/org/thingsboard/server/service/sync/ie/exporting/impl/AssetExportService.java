// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class AssetExportService extends BaseEntityExportService<AssetId, Asset, EntityExportData<Asset>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Asset asset, EntityExportData<Asset> exportData) {
        asset.setCustomerId(getExternalIdOrElseInternal(ctx, asset.getCustomerId()));
        asset.setAssetProfileId(getExternalIdOrElseInternal(ctx, asset.getAssetProfileId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ASSET);
    }

}
