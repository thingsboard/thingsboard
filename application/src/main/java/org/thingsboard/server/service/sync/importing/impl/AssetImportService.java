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
package org.thingsboard.server.service.sync.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.exporting.data.AssetExportData;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetImportService extends BaseEntityImportService<AssetId, Asset, AssetExportData> {

    private final AssetService assetService;

    @Override
    protected Asset prepareAndSave(TenantId tenantId, Asset asset, AssetExportData exportData, NewIdProvider idProvider) {
        asset.setTenantId(tenantId);
        asset.setCustomerId(idProvider.get(tenantId, Asset::getCustomerId));
        return assetService.saveAsset(asset);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
