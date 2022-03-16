/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.expimp.imp.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.export.impl.AssetExportData;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetImportService extends AbstractEntityImportService<AssetId, Asset, AssetExportData> {

    private final AssetService assetService;


    @Override
    public Asset importEntity(TenantId tenantId, AssetExportData exportData) {
        Asset asset = exportData.getAsset();
        Asset existingAsset = findByExternalId(tenantId, asset.getId()); // TODO: extract boiler plate to abstract class ...

        asset.setExternalId(asset.getId());
        asset.setTenantId(tenantId);

        if (existingAsset == null) {
            asset.setId(null);
        } else {
            asset.setId(existingAsset.getId());
        }

        asset.setCustomerId(getInternalId(tenantId, asset.getCustomerId()));

        Asset savedAsset = assetService.saveAsset(asset);

        return savedAsset;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
