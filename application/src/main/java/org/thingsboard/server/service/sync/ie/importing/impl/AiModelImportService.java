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
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class AiModelImportService extends BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>> {

    private final AiModelService aiModelService;

    @Override
    protected void setOwner(
            TenantId tenantId,
            AiModel model,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider
    ) {
        model.setTenantId(tenantId);
    }

    @Override
    protected AiModel prepare(
            EntitiesImportCtx ctx,
            AiModel model,
            AiModel oldModel,
            EntityExportData<AiModel> exportData,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider
    ) {
        return model;
    }

    @Override
    protected AiModel deepCopy(AiModel model) {
        return new AiModel(model);
    }

    @Override
    protected AiModel saveOrUpdate(
            EntitiesImportCtx ctx,
            AiModel model,
            EntityExportData<AiModel> exportData,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider,
            CompareResult compareResult
    ) {
        return aiModelService.save(model);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

}
