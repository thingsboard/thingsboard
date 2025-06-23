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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.id.AiModelSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.ai.AiModelSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class AiModelSettingsImportService extends BaseEntityImportService<AiModelSettingsId, AiModelSettings, EntityExportData<AiModelSettings>> {

    private final AiModelSettingsService aiModelSettingsService;

    @Override
    protected void setOwner(
            TenantId tenantId,
            AiModelSettings settings,
            BaseEntityImportService<AiModelSettingsId, AiModelSettings, EntityExportData<AiModelSettings>>.IdProvider idProvider
    ) {
        settings.setTenantId(tenantId);
    }

    @Override
    protected AiModelSettings prepare(
            EntitiesImportCtx ctx,
            AiModelSettings settings,
            AiModelSettings oldEntity,
            EntityExportData<AiModelSettings> exportData,
            BaseEntityImportService<AiModelSettingsId, AiModelSettings, EntityExportData<AiModelSettings>>.IdProvider idProvider
    ) {
        return settings;
    }

    @Override
    protected AiModelSettings deepCopy(AiModelSettings settings) {
        return new AiModelSettings(settings);
    }

    @Override
    protected AiModelSettings saveOrUpdate(
            EntitiesImportCtx ctx,
            AiModelSettings settings,
            EntityExportData<AiModelSettings> exportData,
            BaseEntityImportService<AiModelSettingsId, AiModelSettings, EntityExportData<AiModelSettings>>.IdProvider idProvider,
            CompareResult compareResult
    ) {
        return aiModelSettingsService.save(settings);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL_SETTINGS;
    }

}
