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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.sync.ie.AssetProfileExportData;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class AssetProfileExportService extends BaseCalculatedFieldsExportService<AssetProfileId, AssetProfile, AssetProfileExportData> {

    protected AssetProfileExportService(CalculatedFieldService calculatedFieldService) {
        super(calculatedFieldService);
    }

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, AssetProfile assetProfile, AssetProfileExportData exportData) {
        assetProfile.setDefaultDashboardId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultDashboardId()));
        assetProfile.setDefaultRuleChainId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultRuleChainId()));
        assetProfile.setDefaultEdgeRuleChainId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultEdgeRuleChainId()));
        setCalculatedFields(ctx, assetProfile, exportData);
    }

    @Override
    protected AssetProfileExportData newExportData() {
        return new AssetProfileExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ASSET_PROFILE);
    }

}
