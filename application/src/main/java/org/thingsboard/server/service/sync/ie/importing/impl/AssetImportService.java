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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.AssetExportData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
public class AssetImportService extends BaseCalculatedFieldsImportService<AssetId, Asset, AssetExportData> {

    private final AssetService assetService;

    public AssetImportService(CalculatedFieldService calculatedFieldService, AssetService assetService) {
        super(calculatedFieldService);
        this.assetService = assetService;
    }

    @Override
    protected void setOwner(TenantId tenantId, Asset asset, IdProvider idProvider) {
        asset.setTenantId(tenantId);
        asset.setCustomerId(idProvider.getInternalId(asset.getCustomerId()));
    }

    @Override
    protected Asset prepare(EntitiesImportCtx ctx, Asset asset, Asset old, AssetExportData exportData, IdProvider idProvider) {
        asset.setAssetProfileId(idProvider.getInternalId(asset.getAssetProfileId()));
        return asset;
    }

    @Override
    protected Asset saveOrUpdate(EntitiesImportCtx ctx, Asset asset, AssetExportData exportData, IdProvider idProvider) {
        return saveOrUpdateEntity(ctx, asset, exportData, idProvider, assetService::saveAsset);
    }

    @Override
    protected Asset deepCopy(Asset asset) {
        return new Asset(asset);
    }

    @Override
    protected void cleanupForComparison(Asset e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
